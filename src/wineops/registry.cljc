(ns wineops.registry
  "Pure validation functions for wine-manufacturing production
  parameters. These are called by the Governor to independently verify
  physical/operational constraints -- the advisor's confidence is NOT
  sufficient to override these checks.

  All functions here are pure arithmetic/set/boolean predicates with no
  host-clock or I/O calls, so this namespace stays trivially portable
  across Clojure/ClojureScript. Callers that need the current time (see
  `bottling-line-calibration-overdue?`) obtain it themselves via a
  `:clj`/`:cljs` reader-conditional at the call site (see
  `wineops.governor`).")

(defn abv-out-of-tolerance?
  "Independently verify that the batch's actual ABV falls within
  tolerance of the product's declared target ABV. Sits outside the
  tolerance band and the batch risks a federal excise-tax-class
  misclassification (US: 27 CFR 4.36) -- a decision this actor never
  makes on its own; it only proposes logging the observed value so a
  human/tax authority can act."
  [actual-percent target-percent tolerance-percent]
  (or (< actual-percent (- target-percent tolerance-percent))
      (> actual-percent (+ target-percent tolerance-percent))))

(defn residual-sugar-out-of-range?
  "Independently verify that the batch's actual residual sugar falls
  within the product's declared-style window. Outside the window
  indicates the finished wine no longer matches its declared style (dry
  table wine vs. sweet dessert wine, etc.) -- a style/label
  misclassification with real consumer-facing consequences."
  [actual-g-per-l min-g-per-l max-g-per-l]
  (or (< actual-g-per-l min-g-per-l)
      (> actual-g-per-l max-g-per-l)))

(defn volatile-acidity-exceeds-max?
  "Independently verify that the batch's volatile acidity (acetic-acid-
  equivalent spoilage/quality indicator, tracked since antiquity as the
  telltale of unwanted acetobacter/oxidation spoilage) does not exceed
  the product's maximum allowable level."
  [actual-g-per-l max-g-per-l]
  (> actual-g-per-l max-g-per-l))

(defn so2-residue-exceeds-max?
  "Independently verify that the batch's actual total sulfur-dioxide
  residue (ppm, added during crush/pressing/aging as an antimicrobial/
  antioxidant preservative) does not exceed the product's maximum
  allowable level. SO2 residue above the regulatory/product action level
  is one of the most serious food-safety hazards specific to wine
  production -- a hard, un-overridable stop."
  [actual-ppm max-ppm]
  (> actual-ppm max-ppm))

(defn vintage-percent-below-minimum?
  "Independently verify that the batch's actual same-vintage-year
  percentage does not fall below the product's minimum required
  percentage for a legal vintage-date label claim (the \"85% rule\", US
  27 CFR 4.27 / EU Reg (EU) 2019/33 Art. 51)."
  [actual-percent min-percent]
  (< actual-percent min-percent))

(defn bottling-line-calibration-overdue?
  "Independently verify that the bottling-line fill-volume metering
  equipment was calibrated within the last 90 days.
  `last-calibration-epoch-ms` and `now-epoch-ms` are both epoch
  milliseconds -- callers obtain `now` via a `:clj`/`:cljs`
  reader-conditional, keeping this namespace free of any host-clock
  call."
  [last-calibration-epoch-ms now-epoch-ms]
  (> (- now-epoch-ms last-calibration-epoch-ms)
     (* 90 24 60 60 1000)))

(defn fill-volume-variance-excessive?
  "Independently verify that a batch's finished-product fill-volume
  variance (drift from the product's standard-of-fill target, in mL)
  does not exceed the maximum tolerance. Excessive variance indicates the
  bottling-line filler is out of calibration or the standard-of-fill
  (27 CFR 4.72) was not met."
  [actual-variance-ml max-variance-ml]
  (> actual-variance-ml max-variance-ml))

(defn sulfite-label-risk?
  "True when the batch's actual SO2 residue crosses the jurisdiction's
  sulfite-declaration threshold but `:sulfites` is NOT present in
  `declared-allergens` (mislabeling / under-declaration risk -- a
  genuine food-safety hazard for sulfite-sensitive consumers).
  Declaring sulfites when not strictly required is conservative and
  never a risk."
  [so2-ppm threshold-ppm declared-allergens]
  (and (some? so2-ppm) (some? threshold-ppm)
       (>= so2-ppm threshold-ppm)
       (not (contains? (set declared-allergens) :sulfites))))

(defn contamination-detected?
  "Independently verify a batch's contamination-detection result (foreign
  material -- glass/metal fragments from the bottling line -- or a
  positive cork-taint/TCA or Brettanomyces spoilage-marker screen). Any
  detection is a genuine physical/quality hazard -- this predicate simply
  coerces the raw fact to a boolean so the Governor's check functions
  stay uniform in shape with every other independently-verified physical
  constraint in this namespace."
  [actual-detected?]
  (boolean actual-detected?))

(defn sanitation-score-insufficient?
  "Independently verify that the winery/cellar's pre-production
  sanitation score meets the minimum required. Score is 0-100, assessed
  by a third-party auditor against food-safety sanitation standards (a
  significant concern specific to preventing Brettanomyces/acetobacter
  spoilage contamination during fermentation, aging, and bottling)."
  [actual-score min-score-required]
  (< actual-score min-score-required))
