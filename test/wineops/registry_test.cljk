(ns wineops.registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [wineops.registry :as registry]))

;; ──────────────────────── ABV Tolerance ──────────────────────

(deftest abv-out-of-tolerance-test
  (testing "ABV at target with no tolerance returns false"
    (is (false? (registry/abv-out-of-tolerance? 12.5 12.5 1.5))))

  (testing "ABV within tolerance range returns false"
    (is (false? (registry/abv-out-of-tolerance? 13.5 12.5 1.5))))

  (testing "ABV below tolerance returns true (violation)"
    (is (true? (registry/abv-out-of-tolerance? 10.5 12.5 1.5))))

  (testing "ABV above tolerance returns true (violation)"
    (is (true? (registry/abv-out-of-tolerance? 14.5 12.5 1.5)))))

;; ──────────────────────── Residual Sugar ──────────────────────

(deftest residual-sugar-out-of-range-test
  (testing "residual sugar within range returns false (no violation)"
    (is (false? (registry/residual-sugar-out-of-range? 2.0 0.0 4.0))))

  (testing "residual sugar below minimum returns true (violation)"
    (is (true? (registry/residual-sugar-out-of-range? -1.0 0.0 4.0))))

  (testing "residual sugar above maximum returns true (violation)"
    (is (true? (registry/residual-sugar-out-of-range? 10.0 0.0 4.0)))))

;; ──────────────────────── Volatile Acidity ──────────────────────

(deftest volatile-acidity-exceeds-max-test
  (testing "volatile acidity within max returns false (no violation)"
    (is (false? (registry/volatile-acidity-exceeds-max? 0.8 1.2))))

  (testing "volatile acidity at max returns false"
    (is (false? (registry/volatile-acidity-exceeds-max? 1.2 1.2))))

  (testing "volatile acidity exceeding max returns true (violation)"
    (is (true? (registry/volatile-acidity-exceeds-max? 1.5 1.2)))))

;; ──────────────────────── SO2 Residue ──────────────────────

(deftest so2-residue-exceeds-max-test
  (testing "SO2 residue within limit returns false (no violation)"
    (is (false? (registry/so2-residue-exceeds-max? 100 150))))

  (testing "SO2 residue at limit returns false"
    (is (false? (registry/so2-residue-exceeds-max? 150 150))))

  (testing "SO2 residue exceeding limit returns true (violation)"
    (is (true? (registry/so2-residue-exceeds-max? 200 150)))))

;; ──────────────────────── Vintage Percent ──────────────────────

(deftest vintage-percent-below-minimum-test
  (testing "vintage percent at minimum returns false (no violation)"
    (is (false? (registry/vintage-percent-below-minimum? 85 85))))

  (testing "vintage percent above minimum returns false"
    (is (false? (registry/vintage-percent-below-minimum? 100 85))))

  (testing "vintage percent below minimum returns true (violation)"
    (is (true? (registry/vintage-percent-below-minimum? 70 85)))))

;; ──────────────────────── Bottling-Line Calibration ──────────────────────

(deftest bottling-line-calibration-overdue-test
  (testing "recent calibration returns false (no violation)"
    ;; Assume calibrated 30 days ago
    (let [now #?(:clj (System/currentTimeMillis) :cljs (.now js/Date))
          thirty-days-ago (- now (* 30 24 60 60 1000))]
      (is (false? (registry/bottling-line-calibration-overdue? thirty-days-ago now)))))

  (testing "overdue calibration returns true (violation)"
    (let [now #?(:clj (System/currentTimeMillis) :cljs (.now js/Date))
          hundred-days-ago (- now (* 100 24 60 60 1000))]
      (is (true? (registry/bottling-line-calibration-overdue? hundred-days-ago now))))))

;; ──────────────────────── Fill Volume Variance ──────────────────────

(deftest fill-volume-variance-excessive-test
  (testing "variance within tolerance returns false (no violation)"
    (is (false? (registry/fill-volume-variance-excessive? 10 15))))

  (testing "variance at tolerance returns false"
    (is (false? (registry/fill-volume-variance-excessive? 15 15))))

  (testing "variance exceeding tolerance returns true (violation)"
    (is (true? (registry/fill-volume-variance-excessive? 16 15)))))

;; ──────────────────────── Sulfite Labeling ──────────────────────

(deftest sulfite-label-risk-test
  (testing "SO2 below threshold returns false (no risk) regardless of declaration"
    (is (false? (registry/sulfite-label-risk? 5 10 #{}))))

  (testing "SO2 at/above threshold with sulfites declared returns false (no risk)"
    (is (false? (registry/sulfite-label-risk? 15 10 #{:sulfites}))))

  (testing "SO2 at/above threshold without sulfites declared returns true (risk)"
    (is (true? (registry/sulfite-label-risk? 15 10 #{})))))

;; ──────────────────────── Contamination ──────────────────────

(deftest contamination-detected-test
  (testing "no detection returns false"
    (is (false? (registry/contamination-detected? false)))
    (is (false? (registry/contamination-detected? nil))))

  (testing "detection returns true"
    (is (true? (registry/contamination-detected? true)))))

;; ──────────────────────── Sanitation Score ──────────────────────

(deftest sanitation-score-insufficient-test
  (testing "score at minimum returns false (no violation)"
    (is (false? (registry/sanitation-score-insufficient? 75 75))))

  (testing "score above minimum returns false"
    (is (false? (registry/sanitation-score-insufficient? 85 75))))

  (testing "score below minimum returns true (violation)"
    (is (true? (registry/sanitation-score-insufficient? 74 75)))))
