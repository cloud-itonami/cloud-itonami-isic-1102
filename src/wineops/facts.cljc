(ns wineops.facts
  "Reference facts for wine manufacturing: product-style production
  parameters (ABV/residual-sugar/volatile-acidity/SO2/fill-volume/
  vintage-percent windows), jurisdiction sulfite-declaration and
  evidence-checklist requirements. This namespace contains pure lookup
  functions for regulatory/food-safety compliance checks -- the Governor
  calls these to independently validate proposals; the advisor's
  confidence is never sufficient on its own."
  (:require [clojure.set :as set]))

(def product-types
  "Valid wine product categories and their safe production windows.
  `abv-target-percent`/`abv-tolerance-percent` follow the US TTB
  alcoholic-content tolerance bands (27 CFR 4.36): wines declared under
  14% ABV get a +/-1.5-point tolerance, wines declared at 14% ABV or
  above (a different federal excise-tax class) get a tighter +/-1.0-point
  tolerance -- crossing the tolerance band risks the batch being
  reclassified into a different tax class, which this actor never decides
  (see `wineops.governor` op-allowlist). `residual-sugar-min/max-g-per-l`
  is the finished-product sugar window that defines the style (dry table
  wine vs. off-dry sparkling vs. sweet dessert/fortified wine).
  `volatile-acidity-max-g-per-l` is the maximum acetic-acid-equivalent
  spoilage indicator (US legal ceiling is 1.2 g/L for red table wine,
  1.1 g/L for white/rose per 27 CFR 4.21(a); this catalog uses a single
  representative per-style ceiling). `so2-max-ppm` is the maximum
  allowable total sulfur-dioxide residue (all styles stay under the US
  federal ceiling of 350 ppm total SO2 per 27 CFR 24.246; the EU ceiling
  under Reg (EU) 2019/934 Annex I Part A varies similarly by style and
  residual-sugar level -- higher-residual-sugar styles are conventionally
  allowed a higher SO2 ceiling because SO2 is a preservative against
  refermentation). `fill-volume-target/tolerance-ml` is the standard-of-
  fill packaging window (27 CFR 4.72). `vintage-percent-min` is the
  minimum percentage of the finished wine that must come from grapes
  harvested in the labeled vintage year to legally carry a vintage-date
  claim -- the long-standing \"85% rule\" under both US (27 CFR 4.27) and
  EU (Reg (EU) 2019/33 Art. 51) wine law."
  {:wine/still-table
   {:id :wine/still-table
    :name "スティルテーブルワイン"
    :abv-target-percent 12.5
    :abv-tolerance-percent 1.5
    :residual-sugar-min-g-per-l 0.0
    :residual-sugar-max-g-per-l 4.0
    :volatile-acidity-max-g-per-l 1.2
    :so2-max-ppm 150
    :fill-volume-target-ml 750
    :fill-volume-tolerance-ml 15
    :vintage-percent-min 85}

   :wine/sparkling
   {:id :wine/sparkling
    :name "スパークリングワイン"
    :abv-target-percent 12.0
    :abv-tolerance-percent 1.5
    :residual-sugar-min-g-per-l 0.0
    :residual-sugar-max-g-per-l 12.0
    :volatile-acidity-max-g-per-l 1.2
    :so2-max-ppm 185
    :fill-volume-target-ml 750
    :fill-volume-tolerance-ml 15
    :vintage-percent-min 85}

   :wine/dessert-sweet
   {:id :wine/dessert-sweet
    :name "デザートワイン(甘口)"
    :abv-target-percent 13.0
    :abv-tolerance-percent 1.5
    :residual-sugar-min-g-per-l 45.0
    :residual-sugar-max-g-per-l 220.0
    :volatile-acidity-max-g-per-l 1.4
    :so2-max-ppm 250
    :fill-volume-target-ml 375
    :fill-volume-tolerance-ml 10
    :vintage-percent-min 85}

   :wine/fortified
   {:id :wine/fortified
    :name "フォーティファイドワイン(酒精強化)"
    :abv-target-percent 18.0
    :abv-tolerance-percent 1.0
    :residual-sugar-min-g-per-l 20.0
    :residual-sugar-max-g-per-l 120.0
    :volatile-acidity-max-g-per-l 1.5
    :so2-max-ppm 300
    :fill-volume-target-ml 750
    :fill-volume-tolerance-ml 15
    :vintage-percent-min 85}})

(defn product-type-by-id [id]
  (get product-types id))

(def jurisdictions
  "Wine-manufacturing jurisdictions and their sulfite-declaration and
  evidence-checklist requirements. Sulfur dioxide (added as a
  preservative/antioxidant during fermentation and aging) is a regulated
  allergen-adjacent hazard -- the ~10 ppm/mg-per-L \"contains sulfites\"
  declaration threshold is the widely-adopted convention across US
  (TTB/FDA), EU, and Japan (each independently converged on the same
  Codex-Alimentarius-aligned action level, so this catalog uses 10
  uniformly). Japan's wine-specific labeling authority is the National
  Tax Agency (国税庁) under the Liquor Tax Act (酒税法) and, since 2018, the
  果実酒等の製法品質表示基準 (\"Standards for Labeling the Production Method
  and Quality of Fruit Wine, etc.\") -- distinct from 厚生労働省/食品表示法,
  which governs general food labeling but not wine's vintage/varietal/
  origin claims."
  {:jp/nta
   {:id :jp/nta
    :name "日本 (酒税法・国税庁 果実酒等の製法品質表示基準)"
    :sulfite-declaration-required true
    :sulfite-declaration-threshold-ppm 10
    :required-evidence
    [:grape-intake-record
     :fermentation-log
     :abv-test
     :residual-sugar-test
     :volatile-acidity-test
     :so2-residue-test
     :allergen-declaration
     :fill-volume-check]}

   :us/ttb
   {:id :us/ttb
    :name "United States (TTB 27 CFR Part 4 / Part 24)"
    :sulfite-declaration-required true
    :sulfite-declaration-threshold-ppm 10
    :required-evidence
    [:grape-intake-record
     :fermentation-log
     :abv-test
     :residual-sugar-test
     :volatile-acidity-test
     :so2-residue-test
     :allergen-declaration
     :fill-volume-check]}

   :eu/dg-agri
   {:id :eu/dg-agri
    :name "European Union (Reg (EU) 1308/2013 / Reg (EU) 2019/934)"
    :sulfite-declaration-required true
    :sulfite-declaration-threshold-ppm 10
    :required-evidence
    [:grape-intake-record
     :fermentation-log
     :abv-test
     :residual-sugar-test
     :volatile-acidity-test
     :so2-residue-test
     :allergen-declaration
     :fill-volume-check]}})

(defn jurisdiction-by-id [id]
  (get jurisdictions id))

(defn sulfite-declaration-required?
  "True when `so2-ppm` crosses the jurisdiction's sulfite-declaration
  threshold and therefore the finished wine must carry a \"contains
  sulfites\" (or equivalent) allergen-adjacent label. `jurisdiction` may
  be a resolved jurisdiction map or a raw jurisdiction id."
  [jurisdiction so2-ppm]
  (let [j (if (map? jurisdiction) jurisdiction (jurisdiction-by-id jurisdiction))]
    (boolean
     (and j so2-ppm
          (>= so2-ppm (:sulfite-declaration-threshold-ppm j))))))

(defn sulfite-declaration-complete?
  "Verify that when sulfite declaration is required for `so2-ppm` under
  `jurisdiction`, `:sulfites` is present in `declared`. Declaring
  sulfites even when not strictly required is conservative and always
  passes."
  [jurisdiction so2-ppm declared]
  (if (sulfite-declaration-required? jurisdiction so2-ppm)
    (contains? (set declared) :sulfites)
    true))

(defn required-evidence-satisfied?
  "Verify that every item in the jurisdiction's `:required-evidence` list
  is present in `evidence`. `jurisdiction` may be a resolved jurisdiction
  map (as returned by `jurisdiction-by-id`) or a raw jurisdiction id --
  both call conventions are in use (tests pass a resolved map; the
  Governor passes the raw id straight off batch metadata)."
  [jurisdiction evidence]
  (let [j (if (map? jurisdiction) jurisdiction (jurisdiction-by-id jurisdiction))]
    (if-not j
      false
      (set/subset? (set (:required-evidence j)) (set evidence)))))

(defn abv-in-tolerance?
  "Positive-sense convenience predicate: does `percent` fall within
  `product`'s ABV tolerance window (inclusive) around its declared
  target? Crossing the window risks a federal excise-tax-class
  misclassification (see 27 CFR 4.36), which this actor never decides on
  its own -- it only proposes logging the observed value."
  [percent product]
  (boolean
   (and (some? product)
        (let [target (:abv-target-percent product)
              tol (:abv-tolerance-percent product)]
          (and (>= percent (- target tol))
               (<= percent (+ target tol)))))))

(defn residual-sugar-in-range?
  "Positive-sense convenience predicate: does `g-per-l` fall within
  `product`'s residual-sugar window (inclusive) -- the window that
  defines the wine's declared style (dry/off-dry/sweet)?"
  [g-per-l product]
  (boolean
   (and (some? product)
        (>= g-per-l (:residual-sugar-min-g-per-l product))
        (<= g-per-l (:residual-sugar-max-g-per-l product)))))

(defn volatile-acidity-within-max?
  "Positive-sense convenience predicate: does `g-per-l` stay at or below
  `product`'s maximum allowable volatile acidity (acetic-acid-equivalent
  spoilage indicator)?"
  [g-per-l product]
  (boolean
   (and (some? product)
        (<= g-per-l (:volatile-acidity-max-g-per-l product)))))

(defn so2-within-max?
  "Positive-sense convenience predicate: does `ppm` stay at or below
  `product`'s maximum allowable total sulfur-dioxide residue?"
  [ppm product]
  (boolean
   (and (some? product)
        (<= ppm (:so2-max-ppm product)))))

(defn fill-volume-in-range?
  "Positive-sense convenience predicate: does `ml` fall within `product`'s
  standard-of-fill window (target +/- tolerance, inclusive)?"
  [ml product]
  (boolean
   (and (some? product)
        (let [target (:fill-volume-target-ml product)
              tol (:fill-volume-tolerance-ml product)]
          (and (>= ml (- target tol))
               (<= ml (+ target tol)))))))

(defn vintage-percent-meets-minimum?
  "Positive-sense convenience predicate: does `percent` meet or exceed
  `product`'s minimum required same-vintage-year percentage for a legal
  vintage-date label claim?"
  [percent product]
  (boolean
   (and (some? product)
        (>= percent (:vintage-percent-min product)))))
