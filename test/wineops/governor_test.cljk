(ns wineops.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [wineops.governor :as governor]))

(def ^:private now-ms #?(:clj (System/currentTimeMillis) :cljs (.now js/Date)))
(def ^:private ten-days-ago (- now-ms (* 10 24 60 60 1000)))
(def ^:private hundred-days-ago (- now-ms (* 100 24 60 60 1000)))

(def ^:private clean-batch
  {:product-type :wine/still-table
   :jurisdiction :us/ttb
   :abv-percent 12.5
   :residual-sugar-g-per-l 2.0
   :volatile-acidity-g-per-l 0.6
   :so2-ppm 50
   :vintage-percent 90
   :fill-volume-variance-ml 5
   :contamination-detected? false
   :bottling-line-last-calibration-date ten-days-ago
   :sanitation-score 85
   ;; SO2 in clean-batch (50ppm) is well above the 10ppm US/EU/JP
   ;; sulfite-declaration threshold, so the fixture must declare
   ;; :sulfites by default -- otherwise every test built on this fixture
   ;; would spuriously trip :sulfite-label-mismatch regardless of what it
   ;; is actually trying to exercise. sulfite-label-violation-test below
   ;; overrides this explicitly for both the mismatch and match cases.
   :declared-allergens #{:sulfites}
   :evidence-checklist [:grape-intake-record :fermentation-log :abv-test
                        :residual-sugar-test :volatile-acidity-test :so2-residue-test
                        :allergen-declaration :fill-volume-check]})

;; ──────────────────────── Batch Registration (generalized) ──────────────────────

(deftest batch-not-registered-violation-test
  (testing "log-production-batch against an unregistered batch is a hard violation"
    (let [req {:op :log-production-batch :subject "batch-ghost"}
          prop {:cites [{:spec "27 CFR 4.1"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop {})]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :batch-not-registered) (:violations result)))))

  (testing "schedule-maintenance against an unregistered batch is also a hard violation"
    (let [req {:op :schedule-maintenance :subject "batch-ghost"}
          prop {:cites [] :value {} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop {})]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :batch-not-registered) (:violations result)))))

  (testing "a registered batch does not trigger this rule"
    (let [batch-id "batch-001"
          store {:batches {batch-id clean-batch}}
          req {:op :schedule-maintenance :subject batch-id}
          prop {:cites [] :value {} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (not (some #(= (:rule %) :batch-not-registered) (:violations result)))))))

;; ──────────────────────── Hard Violations ──────────────────────

(deftest spec-basis-violation-test
  (testing "proposal with no jurisdiction citation is a hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id clean-batch}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [] :value {:jurisdiction nil}}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :no-spec-basis) (:violations result)))))

  (testing "proposal with proper citation passes spec basis check"
    (let [batch-id "batch-001"
          store {:batches {batch-id clean-batch}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.1"}] :value {:jurisdiction :us/ttb}}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (false? (:hard? result))))))

;; ──────────────────────── ABV Tolerance Violations ──────────────────────

(deftest abv-violation-test
  (testing "batch with ABV out of tolerance triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :abv-percent 15.0)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.36"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :abv-out-of-tolerance) (:violations result)))))

  (testing "batch with ABV in tolerance passes"
    (let [batch-id "batch-001"
          store {:batches {batch-id clean-batch}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.36"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (false? (:hard? result)))))

  (testing "fortified wine has a much tighter ABV tolerance than still table wine"
    (let [batch-id "batch-002"
          store {:batches {batch-id (assoc clean-batch
                                            :product-type :wine/fortified
                                            :residual-sugar-g-per-l 60.0
                                            :so2-ppm 100
                                            :abv-percent 19.2)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.36"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :abv-out-of-tolerance) (:violations result))))))

;; ──────────────────────── Residual Sugar Violations ──────────────────────

(deftest residual-sugar-violation-test
  (testing "batch with residual sugar out of the declared style's window triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :residual-sugar-g-per-l 20.0)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.34"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :residual-sugar-out-of-range) (:violations result))))))

;; ──────────────────────── Volatile Acidity Violations ──────────────────────

(deftest volatile-acidity-violation-test
  (testing "batch with volatile acidity exceeding the product's maximum triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :volatile-acidity-g-per-l 1.5)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.21"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :volatile-acidity-exceeds-max) (:violations result))))))

;; ──────────────────────── SO2 Residue Violations ──────────────────────

(deftest so2-residue-violation-test
  (testing "batch with SO2 residue exceeding the product's limit triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :so2-ppm 200)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 24.246"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :so2-residue-exceeded) (:violations result)))))

  (testing "dessert wine has a much higher SO2 limit than still table wine"
    (let [batch-id "batch-002"
          store {:batches {batch-id (assoc clean-batch
                                            :product-type :wine/dessert-sweet
                                            :residual-sugar-g-per-l 60.0
                                            :abv-percent 13.0
                                            :so2-ppm 230)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 24.246"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (false? (:hard? result))))))

;; ──────────────────────── Vintage Percent Violations ──────────────────────

(deftest vintage-percent-violation-test
  (testing "batch below the vintage-labeling minimum percentage triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :vintage-percent 70)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.27"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :vintage-percent-below-minimum) (:violations result))))))

;; ──────────────────────── Contamination Violations ──────────────────────

(deftest contamination-violation-test
  (testing "batch with detected contamination triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :contamination-detected? true)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.1"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :contamination-detected) (:violations result))))))

;; ──────────────────────── Bottling-Line Calibration Violations ──────────────────────

(deftest bottling-line-calibration-violation-test
  (testing "batch with overdue bottling-line calibration triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :bottling-line-last-calibration-date hundred-days-ago)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.72"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :bottling-line-calibration-overdue) (:violations result))))))

;; ──────────────────────── Fill Volume Variance Violations ──────────────────────

(deftest fill-volume-variance-violation-test
  (testing "batch with excessive fill-volume variance triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :fill-volume-variance-ml 25)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.72"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :fill-volume-variance-excessive) (:violations result))))))

;; ──────────────────────── Sulfite Labeling Violations ──────────────────────

(deftest sulfite-label-violation-test
  (testing "SO2 residue above declaration threshold without a sulfite declaration triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :so2-ppm 15 :declared-allergens #{})}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.32a"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :sulfite-label-mismatch) (:violations result)))))

  (testing "SO2 residue above declaration threshold WITH a sulfite declaration passes"
    (let [batch-id "batch-002"
          store {:batches {batch-id (assoc clean-batch :so2-ppm 15 :declared-allergens #{:sulfites})}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.32a"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (not (some #(= (:rule %) :sulfite-label-mismatch) (:violations result)))))))

;; ──────────────────────── Sanitation Score Violations ──────────────────────

(deftest sanitation-score-violation-test
  (testing "batch with insufficient sanitation score triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch :sanitation-score 60)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.1"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :sanitation-score-insufficient) (:violations result))))))

;; ──────────────────────── Food-Safety Flag Violations ──────────────────────

(deftest food-safety-flag-unresolved-violation-test
  (testing "batch with an unresolved food-safety flag triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id (assoc clean-batch
                                            :safety-concern-raised? true
                                            :safety-concern-resolved? false)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.1"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :food-safety-flag-unresolved) (:violations result)))))

  (testing "batch with a resolved food-safety flag does not trigger this rule"
    (let [batch-id "batch-002"
          store {:batches {batch-id (assoc clean-batch
                                            :safety-concern-raised? true
                                            :safety-concern-resolved? true)}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.1"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (not (some #(= (:rule %) :food-safety-flag-unresolved) (:violations result)))))))

;; ──────────────────────── Escalation (Low Confidence) ──────────────────────

(deftest low-confidence-escalation-test
  (testing "low confidence proposal escalates even when hard checks pass"
    (let [batch-id "batch-001"
          store {:batches {batch-id clean-batch}}
          req {:op :schedule-maintenance :subject batch-id}
          prop {:cites [{:spec "Equipment-Manual"}] :value {:jurisdiction :us/ttb} :confidence 0.5}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (false? (:ok? result)))
      (is (true? (:escalate? result)))
      (is (false? (:hard? result))))))

;; ──────────────────────── High Stakes Escalation ──────────────────────

(deftest high-stakes-escalation-test
  (testing "log-production-batch escalates even when all checks pass"
    (let [batch-id "batch-001"
          store {:batches {batch-id clean-batch}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.1"}] :value {:jurisdiction :us/ttb} :confidence 0.95}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (false? (:ok? result)))
      (is (true? (:escalate? result)))
      (is (false? (:hard? result))))))

;; ──────────────────────── Already Processed Violation ──────────────────────

(deftest already-processed-violation-test
  (testing "batch already processed triggers hard violation"
    (let [batch-id "batch-001"
          store {:batches {batch-id
                           {:product-type :wine/still-table
                            :processed? true}}}
          req {:op :log-production-batch :subject batch-id}
          prop {:cites [{:spec "27 CFR 4.1"}] :value {:jurisdiction :us/ttb} :confidence 0.8}
          result (governor/check req {:actor-id "gov-1"} prop store)]
      (is (true? (:hard? result)))
      (is (some #(= (:rule %) :already-processed) (:violations result))))))
