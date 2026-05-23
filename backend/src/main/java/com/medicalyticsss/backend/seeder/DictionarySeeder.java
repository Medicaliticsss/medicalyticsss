package com.medicalyticsss.backend.seeder;

import com.medicalyticsss.backend.model.TestType;
import com.medicalyticsss.backend.repository.TestTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DictionarySeeder implements CommandLineRunner {

    private final TestTypeRepository testTypeRepository;

    public DictionarySeeder(TestTypeRepository testTypeRepository) {
        this.testTypeRepository = testTypeRepository;
    }

    @Override
    public void run(String... args) {
        // Twardy słownik badań
        List<TestType> authoritativeTests = List.of(
                // --- MORFOLOGIA KRWI ---
                createTest("WBC", "Leukocyty", "Morfologia", "10^9/L", "4.0", "10.0"),
                createTest("RBC", "Erytrocyty", "Morfologia", "10^12/L", "4.2", "5.4"),
                createTest("HGB", "Hemoglobina", "Morfologia", "g/dL", "12.0", "16.0"),
                createTest("HCT", "Hematokryt", "Morfologia", "%", "37.0", "47.0"),
                createTest("MCV", "Średnia objętość krwinki (MCV)", "Morfologia", "fL", "80.0", "100.0"),
                createTest("MCH", "Średnia masa Hb w krwince (MCH)", "Morfologia", "pg", "27.0", "32.0"),
                createTest("MCHC", "Średnie stężenie Hb (MCHC)", "Morfologia", "g/dL", "32.0", "36.0"),
                createTest("PLT", "Płytki krwi", "Morfologia", "10^9/L", "150.0", "400.0"),
                createTest("RDW", "Rozkład objętości erytrocytów", "Morfologia", "%", "11.5", "14.5"),
                createTest("NEUT", "Neutrofile", "Morfologia", "10^9/L", "2.0", "7.0"),
                createTest("LYMPH", "Limfocyty", "Morfologia", "10^9/L", "1.0", "3.0"),
                createTest("MONO", "Monocyty", "Morfologia", "10^9/L", "0.2", "1.0"),
                createTest("EO", "Eozynofile", "Morfologia", "10^9/L", "0.0", "0.5"),
                createTest("BASO", "Bazofile", "Morfologia", "10^9/L", "0.0", "0.1"),
                createTest("RET", "Retikulocyty", "Morfologia", "%", "0.5", "1.5"),

                // --- BIOCHEMIA ---
                createTest("GLU", "Glukoza", "Biochemia", "mg/dL", "70.0", "99.0"),
                createTest("UREA", "Mocznik", "Biochemia", "mg/dL", "15.0", "45.0"),
                createTest("CREA", "Kreatynina", "Biochemia", "mg/dL", "0.5", "1.2"),
                createTest("UA", "Kwas moczowy", "Biochemia", "mg/dL", "3.5", "7.2"),
                createTest("TP", "Białko całkowite", "Biochemia", "g/dL", "6.6", "8.7"),
                createTest("ALB", "Albuminy", "Biochemia", "g/dL", "3.5", "5.2"),
                createTest("BIL-T", "Bilirubina całkowita", "Biochemia", "mg/dL", "0.2", "1.2"),
                createTest("BIL-D", "Bilirubina bezpośrednia", "Biochemia", "mg/dL", "0.0", "0.3"),
                createTest("CRP", "Białko C-reaktywne (CRP)", "Biochemia", "mg/L", "0.0", "5.0"),
                createTest("HBA1C", "Hemoglobina glikowana (HbA1c)", "Biochemia", "%", "4.0", "5.6"),
                createTest("HOMO", "Homocysteina", "Biochemia", "umol/L", "5.0", "15.0"),
                createTest("LACT", "Mleczany", "Biochemia", "mmol/L", "0.5", "2.2"),
                createTest("FRUC", "Fruktozamina", "Biochemia", "umol/L", "200.0", "285.0"),

                // --- ENZYMY ---
                createTest("AST", "Aminotransferaza asparaginianowa (AST)", "Enzymy", "U/L", "5.0", "40.0"),
                createTest("ALT", "Aminotransferaza alaninowa (ALT)", "Enzymy", "U/L", "5.0", "40.0"),
                createTest("ALP", "Fosfataza alkaliczna (ALP)", "Enzymy", "U/L", "40.0", "120.0"),
                createTest("GGTP", "Gamma-glutamylotranspeptydaza", "Enzymy", "U/L", "10.0", "40.0"),
                createTest("LDH", "Dehydrogenaza mleczanowa", "Enzymy", "U/L", "140.0", "280.0"),
                createTest("CK", "Kinaza kreatynowa (CK)", "Enzymy", "U/L", "30.0", "200.0"),
                createTest("CK-MB", "Izoenzym MB kinazy kreatynowej", "Enzymy", "U/L", "0.0", "24.0"),
                createTest("AMYL", "Amylaza", "Enzymy", "U/L", "28.0", "100.0"),
                createTest("LIP", "Lipaza", "Enzymy", "U/L", "13.0", "60.0"),
                createTest("CHOL-E", "Cholinoesteraza", "Enzymy", "U/L", "5300.0", "12900.0"),

                // --- JONOGRAM / MIKROELEMENTY ---
                createTest("NA", "Sód (Na)", "Jonogram", "mmol/L", "135.0", "145.0"),
                createTest("K", "Potas (K)", "Jonogram", "mmol/L", "3.5", "5.1"),
                createTest("CL", "Chlorki (Cl)", "Jonogram", "mmol/L", "98.0", "107.0"),
                createTest("CA", "Wapń całkowity (Ca)", "Jonogram", "mg/dL", "8.5", "10.5"),
                createTest("CA-ION", "Wapń zjonizowany", "Jonogram", "mmol/L", "1.1", "1.3"),
                createTest("MG", "Magnez (Mg)", "Jonogram", "mg/dL", "1.6", "2.6"),
                createTest("P", "Fosfor nieorganiczny (P)", "Jonogram", "mg/dL", "2.5", "4.5"),
                createTest("FE", "Żelazo (Fe)", "Jonogram", "ug/dL", "60.0", "170.0"),
                createTest("CU", "Miedź (Cu)", "Jonogram", "ug/dL", "70.0", "140.0"),
                createTest("ZN", "Cynk (Zn)", "Jonogram", "ug/dL", "70.0", "120.0"),

                // --- LIPIDOGRAM ---
                createTest("CHOL", "Cholesterol całkowity", "Lipidogram", "mg/dL", "115.0", "190.0"),
                createTest("HDL", "Cholesterol HDL", "Lipidogram", "mg/dL", "40.0", "100.0"),
                createTest("LDL", "Cholesterol LDL", "Lipidogram", "mg/dL", "0.0", "115.0"),
                createTest("TG", "Triglicerydy", "Lipidogram", "mg/dL", "0.0", "150.0"),
                createTest("NON-HDL", "Cholesterol nie-HDL", "Lipidogram", "mg/dL", "0.0", "130.0"),

                // --- HORMONY TARCZYCY ---
                createTest("TSH", "Hormon tyreotropowy (TSH)", "Hormony", "uIU/mL", "0.27", "4.20"),
                createTest("FT3", "Wolna trijodotyronina (FT3)", "Hormony", "pg/mL", "2.0", "4.4"),
                createTest("FT4", "Wolna tyroksyna (FT4)", "Hormony", "ng/dL", "0.93", "1.70"),
                createTest("ANTI-TPO", "Przeciwciała anty-TPO", "Immunologia", "IU/mL", "0.0", "34.0"),
                createTest("ANTI-TG", "Przeciwciała anty-TG", "Immunologia", "IU/mL", "0.0", "115.0"),

                // --- HORMONY PŁCIOWE I ROZRODCZOŚĆ ---
                createTest("FSH", "Hormon folikulotropowy (FSH)", "Hormony", "mIU/mL", "1.5", "12.4"),
                createTest("LH", "Hormon luteinizujący (LH)", "Hormony", "mIU/mL", "1.7", "8.6"),
                createTest("E2", "Estradiol", "Hormony", "pg/mL", "12.0", "166.0"),
                createTest("PROG", "Progesteron", "Hormony", "ng/mL", "0.2", "1.5"),
                createTest("PRL", "Prolaktyna", "Hormony", "ng/mL", "4.7", "23.3"),
                createTest("TESTO", "Testosteron całkowity", "Hormony", "ng/dL", "250.0", "800.0"),
                createTest("TESTO-F", "Testosteron wolny", "Hormony", "pg/mL", "1.0", "28.0"),
                createTest("SHBG", "Białko wiążące hormony płciowe", "Hormony", "nmol/L", "18.0", "114.0"),
                createTest("DHEA-S", "Siarczan DHEA", "Hormony", "ug/dL", "35.0", "430.0"),
                createTest("BHCG", "Beta-hCG", "Hormony", "mIU/mL", "0.0", "5.0"),

                // --- INNE HORMONY ---
                createTest("CORT", "Kortyzol (rano)", "Hormony", "ug/dL", "6.2", "19.4"),
                createTest("INS", "Insulina (na czczo)", "Hormony", "uIU/mL", "2.6", "24.9"),
                createTest("PTH", "Parathormon (PTH)", "Hormony", "pg/mL", "15.0", "65.0"),
                createTest("ACTH", "Hormon adrenokortykotropowy", "Hormony", "pg/mL", "7.2", "63.3"),
                createTest("ALD", "Aldosteron", "Hormony", "ng/dL", "3.0", "16.0"),

                // --- GOSPODARKA ŻELAZEM I WITAMINY ---
                createTest("FERR", "Ferrytyna", "Witaminy", "ng/mL", "15.0", "300.0"),
                createTest("TIBC", "Całkowita zdolność wiązania Fe", "Witaminy", "ug/dL", "250.0", "400.0"),
                createTest("TRANS", "Transferryna", "Witaminy", "mg/dL", "200.0", "360.0"),
                createTest("VIT-D3", "Witamina D3 (25-OH)", "Witaminy", "ng/mL", "30.0", "50.0"),
                createTest("VIT-B12", "Witamina B12", "Witaminy", "pg/mL", "197.0", "771.0"),
                createTest("FOLIC", "Kwas foliowy", "Witaminy", "ng/mL", "4.6", "18.7"),

                // --- KOAGULOLOGIA ---
                createTest("PT", "Czas protrombinowy (PT)", "Koagulologia", "sek", "10.0", "14.0"),
                createTest("INR", "Wskaźnik INR", "Koagulologia", "-", "0.8", "1.2"),
                createTest("APTT", "Czas kaolinowo-kefalinowy", "Koagulologia", "sek", "26.0", "40.0"),
                createTest("FIBR", "Fibrynogen", "Koagulologia", "g/L", "1.8", "3.5"),
                createTest("D-DIM", "D-dimery", "Koagulologia", "ug/mL", "0.0", "0.5"),

                // --- MARKERY NOWOTWOROWE ---
                createTest("PSA", "PSA całkowity", "Markery", "ng/mL", "0.0", "4.0"),
                createTest("FPSA", "PSA wolny", "Markery", "ng/mL", "0.0", "1.0"),
                createTest("CEA", "Antygen karcinoembrionalny (CEA)", "Markery", "ng/mL", "0.0", "5.0"),
                createTest("CA125", "Antygen nowotworowy 125", "Markery", "U/mL", "0.0", "35.0"),
                createTest("CA153", "Antygen nowotworowy 15-3", "Markery", "U/mL", "0.0", "25.0"),
                createTest("CA199", "Antygen nowotworowy 19-9", "Markery", "U/mL", "0.0", "37.0"),
                createTest("AFP", "Alfa-fetoproteina (AFP)", "Markery", "IU/mL", "0.0", "5.8"),
                createTest("HE4", "Białko HE4", "Markery", "pmol/L", "0.0", "70.0"),
                createTest("CYFRA", "Cyfra 21-1", "Markery", "ng/mL", "0.0", "3.3"),
                createTest("SCC", "Antygen SCC", "Markery", "ng/mL", "0.0", "1.5"),

                // --- IMMUNOLOGIA I SEROLOGIA ---
                createTest("IGG", "Immunoglobuliny IgG", "Immunologia", "g/L", "7.0", "16.0"),
                createTest("IGA", "Immunoglobuliny IgA", "Immunologia", "g/L", "0.7", "4.0"),
                createTest("IGM", "Immunoglobuliny IgM", "Immunologia", "g/L", "0.4", "2.3"),
                createTest("IGE", "Immunoglobuliny IgE całkowite", "Immunologia", "IU/mL", "0.0", "100.0"),
                createTest("RF", "Czynnik reumatoidalny (RF)", "Immunologia", "IU/mL", "0.0", "14.0"),
                createTest("ASO", "Odczyn antystreptolizynowy (ASO)", "Immunologia", "IU/mL", "0.0", "200.0"),

                // --- PARAMETRY MOCZU (Przykładowe liczbowe) ---
                createTest("U-PH", "pH moczu", "Mocz", "-", "5.0", "8.0"),
                createTest("U-SG", "Ciężar właściwy moczu", "Mocz", "g/cm3", "1.015", "1.025"),
                createTest("U-PRO", "Białko w moczu", "Mocz", "mg/dL", "0.0", "15.0"),
                createTest("U-GLU", "Glukoza w moczu", "Mocz", "mg/dL", "0.0", "15.0"),
                createTest("U-KET", "Ciała ketonowe w moczu", "Mocz", "mg/dL", "0.0", "5.0"),
                createTest("U-URO", "Urobilinogen", "Mocz", "mg/dL", "0.1", "1.0"),
                createTest("U-BIL", "Bilirubina w moczu", "Mocz", "mg/dL", "0.0", "0.2"),

                // --- GAZOMETRIA KRWI ---
                createTest("PH", "pH krwi", "Gazometria", "-", "7.35", "7.45"),
                createTest("PCO2", "Ciśnienie parcjalne CO2", "Gazometria", "mmHg", "35.0", "45.0"),
                createTest("PO2", "Ciśnienie parcjalne O2", "Gazometria", "mmHg", "75.0", "100.0"),
                createTest("HCO3", "Wodorowęglany", "Gazometria", "mmol/L", "22.0", "26.0"),
                createTest("BE", "Nadmiar zasad (Base Excess)", "Gazometria", "mmol/L", "-2.0", "2.0")
        );

        for (TestType authTest : authoritativeTests) {
            TestType existing = testTypeRepository.findByTestCode(authTest.getTestCode()).orElse(null);

            if (existing == null) {
                // Jeśli badania nie ma jeszcze w bazie, dodajemy je
                testTypeRepository.save(authTest);
            } else {
                // MDM: Jeśli badanie istnieje, WYMUSZAMY poprawne normy, nazwy i jednostki.
                // To naprawi historyczne błędy (np. złe normy wgrane ze starych plików CSV).
                existing.setTestName(authTest.getTestName());
                existing.setCategoryName(authTest.getCategoryName());
                existing.setUnit(authTest.getUnit());
                existing.setNormMin(authTest.getNormMin());
                existing.setNormMax(authTest.getNormMax());
                testTypeRepository.save(existing);
            }
        }

        System.out.println("Twardy słownik badań (MDM) został pomyślnie załadowany i zsynchronizowany");
    }

    // Metoda pomocnicza do łatwego tworzenia obiektów badań w kodzie
    private TestType createTest(String code, String name, String category, String unit, String min, String max) {
        TestType t = new TestType();
        t.setTestCode(code);
        t.setTestName(name);
        t.setCategoryName(category);
        t.setUnit(unit);
        t.setNormMin(new BigDecimal(min));
        t.setNormMax(new BigDecimal(max));
        return t;
    }
}