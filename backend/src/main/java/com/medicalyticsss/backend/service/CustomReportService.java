package com.medicalyticsss.backend.service;

import com.medicalyticsss.backend.dto.CustomReportRequest;
import com.medicalyticsss.backend.dto.ReportDataPoint;
import com.medicalyticsss.backend.dto.ReportFilter;
import com.medicalyticsss.backend.enums.ReportField;
import com.medicalyticsss.backend.model.FactTestResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<ReportDataPoint> generateCustomReport(CustomReportRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<FactTestResult> root = query.from(FactTestResult.class);

        // DYNAMICZNE GRUPOWANIE (Oś X)
        List<Selection<?>> selections = new ArrayList<>();
        List<Expression<?>> groupBys = new ArrayList<>();

        if (request.selectColumns() != null) {
            for (ReportField field : request.selectColumns()) {
                Path<?> path = resolvePath(root, field);
                selections.add(path);
                groupBys.add(path);
            }
        }

        // DYNAMICZNE FILTROWANIE (WHERE)
        if (request.filters() != null && !request.filters().isEmpty()) {
            List<Predicate> predicates = new ArrayList<>();
            for (ReportFilter filter : request.filters()) {
                predicates.add(buildPredicate(cb, root, filter));
            }
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // DYNAMICZNA AGREGACJA (Oś Y)
        Expression<? extends Number> aggregateExpression = null;
        if (request.aggregateColumn() != null && request.operation() != null) {
            Path<Number> targetPath = resolvePath(root, request.aggregateColumn());
            aggregateExpression = getAggregateExpression(cb, targetPath, request.operation());
            selections.add(aggregateExpression);
        }

        query.multiselect(selections);
        if (!groupBys.isEmpty()) {
            query.groupBy(groupBys);
        }

        // DYNAMICZNE SORTOWANIE
        if (request.sortDirection() != null) {
            Expression<?> sortExpr = null;

            // Zawsze staramy się sortować po wyliczonej wartości (Oś Y)
            if (aggregateExpression != null) {
                sortExpr = aggregateExpression;
            } else if (!groupBys.isEmpty()) {
                // Zapasowo, jeśli nie ma osi Y, sortujemy po osi X
                sortExpr = groupBys.get(0);
            }

            if (sortExpr != null) {
                Order order = request.sortDirection().equalsIgnoreCase("DESC")
                        ? cb.desc(sortExpr)
                        : cb.asc(sortExpr);
                query.orderBy(order);
            }
        }

        // WYKONANIE I MAPOWANIE DLA WYKRESU/TABELI AGREGACJI
        List<Object[]> results = entityManager.createQuery(query).getResultList();

        return results.stream().map(row -> {
            StringBuilder labelBuilder = new StringBuilder();
            int i = 0;
            for (; i < row.length - 1; i++) {
                if (i > 0) labelBuilder.append(" - ");
                labelBuilder.append(row[i] != null ? row[i].toString() : "Brak");
            }
            String label = labelBuilder.length() > 0 ? labelBuilder.toString() : "Ogółem";
            Number value = row[i] != null ? (Number) row[i] : 0;

            return new ReportDataPoint(label, value);
        }).collect(Collectors.toList());
    }

    //POBIERANIE SUROWYCH DANYCH (SELECT *)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRawData(List<ReportFilter> filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FactTestResult> query = cb.createQuery(FactTestResult.class);
        Root<FactTestResult> root = query.from(FactTestResult.class);

        // Nakładamy te same filtry co na wykresie
        if (filters != null && !filters.isEmpty()) {
            List<Predicate> predicates = new ArrayList<>();
            for (ReportFilter filter : filters) {
                predicates.add(buildPredicate(cb, root, filter));
            }
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        query.orderBy(cb.desc(root.get("id")));

        // Limit dla bezpieczeństwa pamięci
        List<FactTestResult> results = entityManager.createQuery(query)
                .setMaxResults(500)
                .getResultList();

        // Budowanie dynamicznego słownika
        return results.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();

            row.put("ID Wyniku", r.getId());

            if (r.getPatient() != null) {
                row.put("Płeć", r.getPatient().getGender() != null ? r.getPatient().getGender().name() : "");
                row.put("Rok Ur.", r.getPatient().getBirthYear());
            }

            if (r.getFacility() != null) {
                row.put("Placówka", r.getFacility().getFacilityName());
                row.put("Miasto", r.getFacility().getCity());
                row.put("Województwo", r.getFacility().getProvince());
            }

            if (r.getTestType() != null) {
                row.put("Kategoria", r.getTestType().getCategoryName());
                row.put("Kod Badania", r.getTestType().getTestCode());
                row.put("Nazwa Badania", r.getTestType().getTestName());
            }

            row.put("Wynik Pacjenta", r.getResultValue());
            row.put("Czy Anomalia?", r.getIsAbnormal());

            return row;
        }).collect(Collectors.toList());
    }

    // TŁUMACZ PÓL
    @SuppressWarnings("unchecked")
    private <T> Path<T> resolvePath(Root<FactTestResult> root, ReportField field) {
        return switch (field) {
            case PATIENT_GENDER -> (Path<T>) root.join("patient", JoinType.LEFT).get("gender");
            case PATIENT_BIRTH_YEAR -> (Path<T>) root.join("patient", JoinType.LEFT).get("birthYear");
            case FACILITY_NAME -> (Path<T>) root.join("facility", JoinType.LEFT).get("facilityName");
            case FACILITY_CITY -> (Path<T>) root.join("facility", JoinType.LEFT).get("city");
            case FACILITY_PROVINCE -> (Path<T>) root.join("facility", JoinType.LEFT).get("province");
            case TEST_CODE -> (Path<T>) root.join("testType", JoinType.LEFT).get("testCode");
            case TEST_NAME -> (Path<T>) root.join("testType", JoinType.LEFT).get("testName");
            case TEST_CATEGORY -> (Path<T>) root.join("testType", JoinType.LEFT).get("categoryName");
            case RESULT_VALUE -> (Path<T>) root.get("resultValue");
            case IS_ABNORMAL -> (Path<T>) root.get("isAbnormal");
        };
    }

    // TŁUMACZ OPERACJI MATEMATYCZNYCH
    private Expression<? extends Number> getAggregateExpression(CriteriaBuilder cb, Path<Number> targetPath, String operation) {
        return switch (operation.toUpperCase()) {
            case "COUNT" -> cb.count(targetPath);
            case "SUM" -> cb.sum(targetPath);
            case "AVG" -> cb.avg(targetPath);
            default -> throw new IllegalArgumentException("Nieobsługiwana operacja: " + operation);
        };
    }

    // BUDOWANIE FILTRÓW
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildPredicate(CriteriaBuilder cb, Root<FactTestResult> root, ReportFilter filter) {
        Path<?> path = resolvePath(root, filter.field());
        Object typedValue = castToRequiredType(path.getJavaType(), filter.value());

        return switch (filter.operator()) {
            case EQUALS -> cb.equal(path, typedValue);
            case NOT_EQUALS -> cb.notEqual(path, typedValue);
            case GREATER_THAN -> cb.greaterThan((Expression<Comparable>) path, (Comparable) typedValue);
            case LESS_THAN -> cb.lessThan((Expression<Comparable>) path, (Comparable) typedValue);
            case CONTAINS -> cb.like(cb.lower((Expression<String>) path), "%" + filter.value().toLowerCase() + "%");
        };
    }

    // RZUTOWANIE TYPÓW
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object castToRequiredType(Class<?> fieldType, String value) {
        if (value == null || value.equalsIgnoreCase("null")) return null;
        if (fieldType.equals(String.class)) return value;
        if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) return Boolean.parseBoolean(value);
        if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) return Integer.parseInt(value);
        if (fieldType.equals(Long.class) || fieldType.equals(long.class)) return Long.parseLong(value);
        if (fieldType.equals(Double.class) || fieldType.equals(double.class)) return Double.parseDouble(value);
        if (fieldType.equals(BigDecimal.class)) return new BigDecimal(value);
        if (fieldType.isEnum()) {
            return Enum.valueOf((Class<Enum>) fieldType, value.toUpperCase());
        }
        return value;
    }
}