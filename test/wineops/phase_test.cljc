(ns wineops.phase-test
  (:require [clojure.test :refer [deftest is testing]]
            [wineops.phase :as phase]))

;; ──────────────────────── Phase Validity ──────────────────────

(deftest valid-phase-test
  (testing "intake is valid"
    (is (true? (phase/valid-phase? :intake))))

  (testing "fermentation is valid"
    (is (true? (phase/valid-phase? :fermentation))))

  (testing "archived is valid"
    (is (true? (phase/valid-phase? :archived))))

  (testing "invalid phase returns false"
    (is (false? (phase/valid-phase? :invalid)))))

;; ──────────────────────── Phase Transitions ──────────────────────

(deftest can-transition-test
  (testing "intake -> crush is valid (forward progression)"
    (is (true? (phase/can-transition? :intake :crush))))

  (testing "intake -> fermentation is valid (skip crush)"
    (is (true? (phase/can-transition? :intake :fermentation))))

  (testing "crush -> intake is invalid (backward)"
    (is (false? (phase/can-transition? :crush :intake))))

  (testing "fermentation -> archived is valid (forward to end)"
    (is (true? (phase/can-transition? :fermentation :archived))))

  (testing "archived -> intake is invalid (backward from end)"
    (is (false? (phase/can-transition? :archived :intake))))

  (testing "same phase is invalid"
    (is (false? (phase/can-transition? :fermentation :fermentation))))

  (testing "invalid phases return false"
    (is (false? (phase/can-transition? :invalid :fermentation)))
    (is (false? (phase/can-transition? :fermentation :invalid)))))
