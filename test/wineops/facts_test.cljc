(ns wineops.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [wineops.facts :as facts]))

;; ──────────────────────── Product Type Lookups ──────────────────────

(deftest product-type-by-id-test
  (testing "still table wine product type exists"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (some? p))
      (is (= (:id p) :wine/still-table))
      (is (= (:abv-target-percent p) 12.5))
      (is (= (:so2-max-ppm p) 150))))

  (testing "sparkling wine product type exists"
    (let [p (facts/product-type-by-id :wine/sparkling)]
      (is (some? p))
      (is (= (:residual-sugar-max-g-per-l p) 12.0))
      (is (= (:so2-max-ppm p) 185))))

  (testing "dessert sweet wine product type exists"
    (let [p (facts/product-type-by-id :wine/dessert-sweet)]
      (is (some? p))
      (is (= (:residual-sugar-min-g-per-l p) 45.0))
      (is (= (:so2-max-ppm p) 250))))

  (testing "fortified wine product type exists with tighter ABV tolerance"
    (let [p (facts/product-type-by-id :wine/fortified)]
      (is (some? p))
      (is (= (:abv-target-percent p) 18.0))
      (is (= (:abv-tolerance-percent p) 1.0))))

  (testing "nonexistent product type returns nil"
    (is (nil? (facts/product-type-by-id :wine/nonexistent)))))

;; ──────────────────────── Jurisdiction Lookups ──────────────────────

(deftest jurisdiction-by-id-test
  (testing "JP NTA jurisdiction exists"
    (let [j (facts/jurisdiction-by-id :jp/nta)]
      (is (some? j))
      (is (true? (:sulfite-declaration-required j)))
      (is (= (:sulfite-declaration-threshold-ppm j) 10))))

  (testing "US TTB jurisdiction exists"
    (let [j (facts/jurisdiction-by-id :us/ttb)]
      (is (some? j))
      (is (= (:sulfite-declaration-threshold-ppm j) 10))))

  (testing "EU DG-AGRI jurisdiction exists"
    (let [j (facts/jurisdiction-by-id :eu/dg-agri)]
      (is (some? j))
      (is (= (:sulfite-declaration-threshold-ppm j) 10))))

  (testing "nonexistent jurisdiction returns nil"
    (is (nil? (facts/jurisdiction-by-id :xx/unknown)))))

;; ──────────────────────── Sulfite Declaration ──────────────────────

(deftest sulfite-declaration-required-test
  (testing "SO2 residue above threshold requires declaration"
    (is (true? (facts/sulfite-declaration-required? :us/ttb 15))))

  (testing "SO2 residue at threshold requires declaration"
    (is (true? (facts/sulfite-declaration-required? :us/ttb 10))))

  (testing "SO2 residue below threshold does not require declaration"
    (is (false? (facts/sulfite-declaration-required? :us/ttb 5))))

  (testing "accepts a resolved jurisdiction map"
    (let [j (facts/jurisdiction-by-id :jp/nta)]
      (is (false? (facts/sulfite-declaration-required? j 5)))
      (is (true? (facts/sulfite-declaration-required? j 10))))))

(deftest sulfite-declaration-complete-test
  (testing "declaration present when required passes"
    (is (true? (facts/sulfite-declaration-complete? :us/ttb 15 #{:sulfites}))))

  (testing "declaration missing when required fails"
    (is (false? (facts/sulfite-declaration-complete? :us/ttb 15 #{}))))

  (testing "declaration not required when SO2 below threshold passes regardless"
    (is (true? (facts/sulfite-declaration-complete? :us/ttb 5 #{}))))

  (testing "declaring sulfites even when not required is conservative and passes"
    (is (true? (facts/sulfite-declaration-complete? :us/ttb 5 #{:sulfites})))))

;; ──────────────────────── Evidence Completeness ──────────────────────

(deftest required-evidence-satisfied-test
  (testing "complete evidence checklist passes"
    (let [j (facts/jurisdiction-by-id :us/ttb)
          evidence [:grape-intake-record :fermentation-log :abv-test
                    :residual-sugar-test :volatile-acidity-test :so2-residue-test
                    :allergen-declaration :fill-volume-check]]
      (is (true? (facts/required-evidence-satisfied? j evidence)))))

  (testing "incomplete evidence fails"
    (let [j (facts/jurisdiction-by-id :us/ttb)
          evidence [:grape-intake-record :fermentation-log]]
      (is (false? (facts/required-evidence-satisfied? j evidence))))))

;; ──────────────────────── Production Safety Predicates ──────────────────────

(deftest abv-in-tolerance-test
  (testing "ABV at target passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/abv-in-tolerance? 12.5 p)))))

  (testing "ABV at lower tolerance boundary passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/abv-in-tolerance? 11.0 p)))))

  (testing "ABV below tolerance fails"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (false? (facts/abv-in-tolerance? 10.9 p)))))

  (testing "ABV above tolerance fails"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (false? (facts/abv-in-tolerance? 14.1 p))))))

(deftest residual-sugar-in-range-test
  (testing "residual sugar within style window passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/residual-sugar-in-range? 2.0 p)))))

  (testing "residual sugar below style window fails"
    (let [p (facts/product-type-by-id :wine/dessert-sweet)]
      (is (false? (facts/residual-sugar-in-range? 10.0 p)))))

  (testing "residual sugar above style window fails"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (false? (facts/residual-sugar-in-range? 10.0 p))))))

(deftest volatile-acidity-within-max-test
  (testing "volatile acidity at or below max passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/volatile-acidity-within-max? 1.2 p)))
      (is (true? (facts/volatile-acidity-within-max? 0.6 p)))))

  (testing "volatile acidity above max fails"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (false? (facts/volatile-acidity-within-max? 1.5 p))))))

(deftest so2-within-max-test
  (testing "SO2 at or below the max passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/so2-within-max? 150 p)))
      (is (true? (facts/so2-within-max? 50 p)))))

  (testing "SO2 above the max fails"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (false? (facts/so2-within-max? 200 p))))))

(deftest fill-volume-in-range-test
  (testing "fill volume at target passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/fill-volume-in-range? 750 p)))))

  (testing "fill volume within tolerance passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/fill-volume-in-range? 760 p)))))

  (testing "fill volume outside tolerance fails"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (false? (facts/fill-volume-in-range? 700 p))))))

(deftest vintage-percent-meets-minimum-test
  (testing "vintage percent at minimum passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/vintage-percent-meets-minimum? 85 p)))))

  (testing "vintage percent above minimum passes"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (true? (facts/vintage-percent-meets-minimum? 100 p)))))

  (testing "vintage percent below minimum fails"
    (let [p (facts/product-type-by-id :wine/still-table)]
      (is (false? (facts/vintage-percent-meets-minimum? 70 p))))))

;; ───────── Verified US CFR figures (2026-07-25) ─────────

(deftest us-limits-all-carry-a-fetched-source
  (is (true? (facts/us-limits-cited?)))
  (doseq [[k v] facts/us-regulatory-limits]
    (is (re-find #"^27 CFR " (:section v)) (str k " must name its CFR section"))
    (is (re-find #"govinfo\.gov" (:provenance v)) (str k " must cite official CFR XML"))))

(deftest abv-tolerance-boundary-is-not-inclusive-of-14
  (testing "4.36(b): MORE THAN 14% gets 1.0; 14% OR LESS gets 1.5"
    (is (= 1.5 (facts/abv-tolerance-percent 14.0))
        "exactly 14.0 falls in the 'or less' band -- the docstring had this backwards")
    (is (= 1.5 (facts/abv-tolerance-percent 13.9)))
    (is (= 1.0 (facts/abv-tolerance-percent 14.1)))
    (is (nil? (facts/abv-tolerance-percent nil)))))

(deftest volatile-acidity-figures-match-27-cfr-4-21
  (testing "0.14 g/100mL red = 1.4 g/L; 0.12 = 1.2 g/L -- not the 1.2/1.1 claimed before"
    (is (= 1.4 (facts/volatile-acidity-ceiling-g-per-l :red)))
    (is (= 1.2 (facts/volatile-acidity-ceiling-g-per-l :other))))
  (testing "unameliorated 28+ Brix allowance"
    (is (= 1.7 (facts/volatile-acidity-ceiling-g-per-l :red true)))
    (is (= 1.5 (facts/volatile-acidity-ceiling-g-per-l :other true))))
  (is (nil? (facts/volatile-acidity-ceiling-g-per-l :chartreuse))))

(deftest every-style-va-ceiling-stays-within-the-statutory-maximum
  ;; The highest figure 4.21 permits at all is 1.7 g/L (red, unameliorated 28+
  ;; Brix). No configured style may exceed that.
  (let [ceiling (facts/volatile-acidity-ceiling-g-per-l :red true)]
    (doseq [[id pt] facts/product-types]
      (is (<= (:volatile-acidity-max-g-per-l pt) ceiling)
          (str id " VA ceiling must not exceed the statutory maximum")))))

(deftest so2-figure-is-attributed-to-the-section-that-states-it
  (let [so2 (:total-so2 facts/us-regulatory-limits)]
    (is (= 350.0 (:max-ppm so2)))
    (is (= "27 CFR 4.22(b)(1)" (:section so2))
        "the figure is in 4.22; 24.246 only delegates to it")
    (is (= "27 CFR 24.246" (:delegated-from so2))))
  (testing "every style stays under the federal ceiling"
    (doseq [[id pt] facts/product-types]
      (is (<= (:so2-max-ppm pt) 350) (str id " must stay under 350 ppm")))))

(deftest vintage-rule-is-appellation-dependent-not-a-flat-85
  (testing "4.27(a): 95% for a viticultural area, 85% otherwise"
    (is (= 95 (facts/vintage-minimum-percent :viticultural-area)))
    (is (= 85 (facts/vintage-minimum-percent :other-appellation)))
    (is (nil? (facts/vintage-minimum-percent :unknown))
        "an unrecognised appellation must not default to the permissive value"))

  (testing "the catalog's flat 85 is permissive for AVA wine, and says so"
    (doseq [id (keys facts/product-types)]
      (is (false? (facts/style-vintage-minimum-meets-ava-rule? id))
          (str id " carries 85, which does not meet the 95% AVA rule")))
    (is (= [] (:styles-meeting-ava-vintage-rule (facts/citation-coverage))))))

(deftest fill-standards-are-sizes-not-a-tolerance
  (let [f (:standards-of-fill facts/us-regulatory-limits)]
    (is (some #{750} (:authorized-ml f)))
    (is (false? (:tolerance-band-defined? f))
        "4.72 authorizes discrete sizes; the tolerance is this actor's own")))

(deftest citation-coverage-reports-eu-as-uncited
  (let [c (facts/citation-coverage)]
    (is (true? (:us-cited? c)))
    (is (false? (:eu-figures-cited? c))
        "no EU source was fetched in this pass, so it must not be reported as cited")))
