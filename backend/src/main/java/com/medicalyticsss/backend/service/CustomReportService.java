package com.medicalyticsss.backend.service;

import com.medicalyticsss.backend.dto.CustomReportRequest;
import com.medicalyticsss.backend.dto.ReportDataPoint;
import com.medicalyticsss.backend.dto.ReportFilter;
import com.medicalyticsss.backend.dto.SeriesReportDataPoint;
import com.medicalyticsss.backend.dto.SeriesReportRequest;
import com.medicalyticsss.backend.enums.ReportField;
import com.medicalyticsss.backend.model.FactTestResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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

        // DYNAMICZNA AGREGACJA (Oś Y). Gdy jej nie podano, endpoint wykresowy pokazuje liczność grup.
        Expression<? extends Number> aggregateExpression = hasAggregate(request)
                ? getAggregateExpression(cb, root, request.aggregateColumn(), request.operation())
                : cb.count(root);
        selections.add(aggregateExpression);

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

    // ELASTYCZNA TABELA RAPORTOWA: obsługuje agregację opcjonalną
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateCustomReportRows(CustomReportRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<FactTestResult> root = query.from(FactTestResult.class);

        List<ReportField> selectedFields = request.selectColumns() != null
                ? request.selectColumns()
                : List.of();
        boolean aggregateRequested = hasAggregate(request);

        if (selectedFields.isEmpty() && !aggregateRequested) {
            throw new IllegalArgumentException("Wybierz co najmniej jedną kolumnę albo agregację");
        }

        List<Selection<?>> selections = new ArrayList<>();
        List<Expression<?>> groupBys = new ArrayList<>();

        for (ReportField field : selectedFields) {
            Path<?> path = resolvePath(root, field);
            selections.add(path);
            groupBys.add(path);
        }

        String aggregateLabel = null;
        Expression<? extends Number> aggregateExpression = null;
        if (aggregateRequested) {
            aggregateExpression = getAggregateExpression(cb, root, request.aggregateColumn(), request.operation());
            selections.add(aggregateExpression);
            aggregateLabel = getAggregateLabel(request.operation(), request.aggregateColumn());
        } else {
            query.distinct(true);
        }

        applyFilters(cb, query, root, request.filters());
        query.multiselect(selections);
        if (aggregateRequested && !groupBys.isEmpty()) {
            query.groupBy(groupBys);
        }

        applySort(cb, query, root, request, groupBys, aggregateExpression);

        List<Object[]> results = entityManager.createQuery(query).getResultList();
        String finalAggregateLabel = aggregateLabel;
        return results.stream()
                .map(row -> mapReportRow(row, selectedFields, finalAggregateLabel))
                .collect(Collectors.toList());
    }

    // WIELOSERYJNE DANE DO WYKRESÓW, np. X=rok urodzenia, seria=kod badania lipidogramu
    @Transactional(readOnly = true)
    public List<SeriesReportDataPoint> generateSeriesReport(SeriesReportRequest request) {
        if (request.xAxis() == null || request.seriesField() == null) {
            throw new IllegalArgumentException("Raport seryjny wymaga pól xAxis i seriesField");
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<FactTestResult> root = query.from(FactTestResult.class);

        Path<?> xPath = resolvePath(root, request.xAxis());
        Path<?> seriesPath = resolvePath(root, request.seriesField());
        String operation = request.operation() != null ? request.operation() : "COUNT";
        Expression<? extends Number> aggregateExpression =
                getAggregateExpression(cb, root, request.aggregateColumn(), operation);

        applyFilters(cb, query, root, request.filters());
        query.multiselect(xPath, seriesPath, aggregateExpression);
        query.groupBy(xPath, seriesPath);

        boolean desc = request.sortDirection() != null && request.sortDirection().equalsIgnoreCase("DESC");
        query.orderBy(desc ? cb.desc(xPath) : cb.asc(xPath), cb.asc(seriesPath));

        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new SeriesReportDataPoint(formatValue(row[0]), formatValue(row[1]), (Number) row[2]))
                .collect(Collectors.toList());
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

    private void applyFilters(CriteriaBuilder cb, CriteriaQuery<?> query, Root<FactTestResult> root, List<ReportFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }

        List<Predicate> predicates = new ArrayList<>();
        for (ReportFilter filter : filters) {
            predicates.add(buildPredicate(cb, root, filter));
        }
        query.where(cb.and(predicates.toArray(new Predicate[0])));
    }

    private void applySort(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<FactTestResult> root,
            CustomReportRequest request,
            List<Expression<?>> groupBys,
            Expression<? extends Number> aggregateExpression
    ) {
        if (request.sortDirection() == null) {
            return;
        }

        Expression<?> sortExpr = null;
        if (request.sortByColumn() != null) {
            sortExpr = resolvePath(root, request.sortByColumn());
        } else if (aggregateExpression != null) {
            sortExpr = aggregateExpression;
        } else if (!groupBys.isEmpty()) {
            sortExpr = groupBys.get(0);
        }

        if (sortExpr != null) {
            Order order = request.sortDirection().equalsIgnoreCase("DESC")
                    ? cb.desc(sortExpr)
                    : cb.asc(sortExpr);
            query.orderBy(order);
        }
    }

    private Map<String, Object> mapReportRow(Object[] row, List<ReportField> selectedFields, String aggregateLabel) {
        Map<String, Object> result = new LinkedHashMap<>();
        int index = 0;
        for (ReportField field : selectedFields) {
            result.put(field.name(), row[index++]);
        }

        if (aggregateLabel != null) {
            result.put(aggregateLabel, row[index]);
        }

        return result;
    }

    private boolean hasAggregate(CustomReportRequest request) {
        return request.operation() != null
                && !request.operation().isBlank()
                && !request.operation().equalsIgnoreCase("NONE");
    }

    private String getAggregateLabel(String operation, ReportField aggregateColumn) {
        String columnName = aggregateColumn != null ? aggregateColumn.name() : "ROWS";
        return operation.toUpperCase() + "_" + columnName;
    }

    private String formatValue(Object value) {
        return value != null ? value.toString() : "Brak";
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
    @SuppressWarnings("unchecked")
    private Expression<? extends Number> getAggregateExpression(
            CriteriaBuilder cb,
            Root<FactTestResult> root,
            ReportField aggregateColumn,
            String operation
    ) {
        return switch (operation.toUpperCase()) {
            case "COUNT" -> aggregateColumn != null ? cb.count(resolvePath(root, aggregateColumn)) : cb.count(root);
            case "SUM" -> cb.sum(resolveNumericPath(root, requireAggregateColumn(aggregateColumn, operation), operation));
            case "AVG" -> cb.avg(resolveNumericPath(root, requireAggregateColumn(aggregateColumn, operation), operation));
            default -> throw new IllegalArgumentException("Nieobsługiwana operacja: " + operation);
        };
    }

    @SuppressWarnings("unchecked")
    private Path<Number> resolveNumericPath(Root<FactTestResult> root, ReportField field, String operation) {
        Path<?> path = resolvePath(root, field);
        if (!Number.class.isAssignableFrom(path.getJavaType())) {
            throw new IllegalArgumentException("Operacja " + operation + " wymaga pola liczbowego");
        }
        return (Path<Number>) path;
    }

    private ReportField requireAggregateColumn(ReportField aggregateColumn, String operation) {
        if (aggregateColumn == null) {
            throw new IllegalArgumentException("Operacja " + operation + " wymaga kolumny agregowanej");
        }
        return aggregateColumn;
    }

    // BUDOWANIE FILTRÓW
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildPredicate(CriteriaBuilder cb, Root<FactTestResult> root, ReportFilter filter) {
        Path<?> path = resolvePath(root, filter.field());
        Object typedValue = filter.operator() != com.medicalyticsss.backend.enums.FilterOperator.IN
                && filter.operator() != com.medicalyticsss.backend.enums.FilterOperator.BETWEEN
                ? castToRequiredType(path.getJavaType(), filter.value())
                : null;

        return switch (filter.operator()) {
            case EQUALS -> cb.equal(path, typedValue);
            case NOT_EQUALS -> cb.notEqual(path, typedValue);
            case GREATER_THAN -> cb.greaterThan((Expression<Comparable>) path, (Comparable) typedValue);
            case LESS_THAN -> cb.lessThan((Expression<Comparable>) path, (Comparable) typedValue);
            case CONTAINS -> cb.like(cb.lower((Expression<String>) path), "%" + filter.value().toLowerCase() + "%");
            case IN -> path.in(parseListValues(path.getJavaType(), filter.value()));
            case BETWEEN -> {
                List<Object> values = parseListValues(path.getJavaType(), filter.value());
                if (values.size() != 2) {
                    throw new IllegalArgumentException("Operator BETWEEN wymaga dwóch wartości rozdzielonych przecinkiem");
                }
                yield cb.between((Expression<Comparable>) path, (Comparable) values.get(0), (Comparable) values.get(1));
            }
        };
    }

    private List<Object> parseListValues(Class<?> fieldType, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Operator listowy wymaga wartości");
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> castToRequiredType(fieldType, v))
                .collect(Collectors.toList());
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