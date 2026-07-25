(ns wineops.facts
  "Reference facts for wine manufacturing: product-style production
  parameters (ABV/residual-sugar/volatile-acidity/SO2/fill-volume/
  vintage-percent windows), jurisdiction sulfite-declaration and
  evidence-checklist requirements. This namespace contains pure lookup
  functions for regulatory/food-safety compliance checks -- the Governor
  calls these to independently validate proposals; the advisor's
  confidence is never sufficient on its own."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

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
  spoilage indicator. `so2-max-ppm` is the maximum allowable total
  sulfur-dioxide residue. `fill-volume-target/tolerance-ml` is the
  packaging window. `vintage-percent-min` is the minimum percentage of the
  finished wine that must come from grapes harvested in the labeled
  vintage year to carry a vintage-date claim.

  CITATION PROVENANCE (2026-07-25). Read out of govinfo.gov's official CFR
  XML and re-grepped against the raw markup. See `us-regulatory-limits`
  below for the machine-readable figures. THREE of the prose claims this
  docstring previously made were wrong, and are corrected here:

    1. ABV TOLERANCE BOUNDARY. It said wines \"at 14% ABV or above\" get the
       tighter 1.0-point tolerance. 27 CFR 4.36(b) says the opposite at the
       boundary: verbatim, \"a tolerance of 1 percent, in the case of wines
       containing MORE THAN 14 percent of alcohol by volume, and of 1.5
       percent, in the case of wines containing 14 percent OR LESS of
       alcohol by volume\". A wine at exactly 14.0% gets 1.5, not 1.0.
    2. VOLATILE ACIDITY FIGURES. It said \"1.2 g/L for red table wine,
       1.1 g/L for white/rose per 27 CFR 4.21(a)\". Both numbers are wrong.
       27 CFR 4.21 verbatim: \"The maximum volatile acidity, calculated as
       acetic acid and exclusive of sulfur dioxide is 0.14 gram per 100 mL
       (20 degrees Celsius) for red wine and 0.12 gram per 100 mL (20
       degrees Celsius) for other grape wine\" -- i.e. 1.4 g/L red and
       1.2 g/L other, each 0.2 g/L higher than claimed. The same paragraph
       allows 0.17 / 0.15 g per 100 mL (1.7 / 1.5 g/L) \"for wine produced
       from unameliorated juice of 28 or more degrees Brix\".
    3. SO2 CITATION. The 350 ppm ceiling was attributed to 27 CFR 24.246.
       That section is \"Materials authorized for the treatment of wine and
       juice\" and does not state the figure -- it delegates: sulfur dioxide
       content \"must not exceed the limitations prescribed in 27 CFR
       4.22(b)(1)\". The figure lives in 4.22, verbatim: \"the presence in
       finished wine of not more than 350 parts per million of total sulfur
       dioxide, or sulphites expressed as sulfur dioxide, shall not be
       precluded\".

  ALSO CORRECTED -- THE VINTAGE RULE IS NOT A FLAT 85%. 27 CFR 4.27(a)
  verbatim: \"If an American or imported wine is labeled with a viticultural
  area appellation of origin (or its foreign equivalent), at least 95
  percent of the wine must have been derived from grapes harvested in the
  labeled calendar year; or ... If ... labeled with an appellation of origin
  other than a viticultural area ..., at least 85 percent\". The catalog's
  flat `:vintage-percent-min 85` is therefore PERMISSIVE for any
  AVA-labelled wine. The per-style values are left untouched because
  `wineops.governor` gates on them and this catalog does not model
  appellation type at all -- tightening every style to 95 would wrongly
  reject legal non-AVA wine at 85-94%. Use
  `vintage-minimum-percent` for the appellation-aware figure; wiring it
  into the Governor needs the proposal to carry an appellation type, which
  is a design change, not a fact fix.

  NOTE ON FILL. 27 CFR 4.72 authorizes discrete standards of fill (\"3
  liters ... 750 milliliters ... 187 milliliters ... 50 milliliters\"), not a
  tolerance band, so `fill-volume-tolerance-ml` is this actor's own
  packaging window and is not attributed to 4.72.

  EU figures (Reg (EU) 2019/934 Annex I Part A for SO2, Reg (EU) 2019/33
  Art. 51 for vintage) are NOT re-verified here -- no EU source was fetched
  in this pass, so they are named as context only and reported as uncited by
  `citation-coverage`."
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

(def us-regulatory-limits
  "US figures verified against govinfo.gov's official CFR XML on 2026-07-25.
  Each entry carries the section it came from so a reader can re-check it."
  {:abv-tolerance
   {:over-14-percent 1.0          ; 4.36(b): "MORE THAN 14 percent" -> 1 percent
    :at-or-under-14-percent 1.5   ; 4.36(b): "14 percent OR LESS"  -> 1.5 percent
    :section "27 CFR 4.36(b)"
    :provenance "https://www.govinfo.gov/content/pkg/CFR-2024-title27-vol1/xml/CFR-2024-title27-vol1-sec4-36.xml"}
   :volatile-acidity
   {:red-g-per-l 1.4                        ; 0.14 g/100 mL at 20 C
    :other-grape-g-per-l 1.2                ; 0.12 g/100 mL
    :red-high-brix-g-per-l 1.7              ; 0.17 g/100 mL, unameliorated 28+ Brix
    :white-high-brix-g-per-l 1.5            ; 0.15 g/100 mL
    :section "27 CFR 4.21"
    :provenance "https://www.govinfo.gov/content/pkg/CFR-2024-title27-vol1/xml/CFR-2024-title27-vol1-sec4-21.xml"}
   :total-so2
   {:max-ppm 350.0
    :section "27 CFR 4.22(b)(1)"
    ;; NOT 24.246 -- that section delegates here rather than stating the figure.
    :delegated-from "27 CFR 24.246"
    :provenance "https://www.govinfo.gov/content/pkg/CFR-2024-title27-vol1/xml/CFR-2024-title27-vol1-sec4-22.xml"}
   :vintage
   {:viticultural-area-percent 95           ; 4.27(a)(1)
    :other-appellation-percent 85           ; 4.27(a)(2)
    :section "27 CFR 4.27(a)"
    :provenance "https://www.govinfo.gov/content/pkg/CFR-2024-title27-vol1/xml/CFR-2024-title27-vol1-sec4-27.xml"}
   :standards-of-fill
   {:authorized-ml [3000 1500 1000 750 500 375 355 250 200 187 100 50]
    :tolerance-band-defined? false          ; 4.72 lists sizes, not a tolerance
    :section "27 CFR 4.72(a)"
    :provenance "https://www.govinfo.gov/content/pkg/CFR-2024-title27-vol1/xml/CFR-2024-title27-vol1-sec4-72.xml"}})

(defn abv-tolerance-percent
  "The 27 CFR 4.36(b) label tolerance for a declared ABV. The boundary is
  load-bearing and was previously documented backwards: exactly 14.0% falls in
  the \"14 percent or less\" band and gets 1.5, not 1.0."
  [declared-abv-percent]
  (when (number? declared-abv-percent)
    (if (> declared-abv-percent 14.0)
      (get-in us-regulatory-limits [:abv-tolerance :over-14-percent])
      (get-in us-regulatory-limits [:abv-tolerance :at-or-under-14-percent]))))

(defn volatile-acidity-ceiling-g-per-l
  "The 27 CFR 4.21 volatile-acidity ceiling. `colour` is :red or :other;
  `high-brix?` selects the unameliorated-28+-Brix allowance."
  ([colour] (volatile-acidity-ceiling-g-per-l colour false))
  ([colour high-brix?]
   (let [va (:volatile-acidity us-regulatory-limits)]
     (case [colour (boolean high-brix?)]
       [:red false]   (:red-g-per-l va)
       [:red true]    (:red-high-brix-g-per-l va)
       [:other false] (:other-grape-g-per-l va)
       [:other true]  (:white-high-brix-g-per-l va)
       nil))))

(defn vintage-minimum-percent
  "The 27 CFR 4.27(a) vintage minimum, which is NOT a flat 85%: an
  appellation that is a viticultural area requires 95%. Returns nil for an
  unrecognised appellation type rather than defaulting to the permissive
  value."
  [appellation-type]
  (let [v (:vintage us-regulatory-limits)]
    (case appellation-type
      :viticultural-area (:viticultural-area-percent v)
      :other-appellation (:other-appellation-percent v)
      nil)))

(defn style-vintage-minimum-meets-ava-rule?
  "Does `product-type-id`'s configured `:vintage-percent-min` satisfy the 95%
  AVA requirement? Makes the known permissiveness machine-checkable instead of
  prose-only: this is false for every style in the catalog today, because they
  all carry 85."
  [product-type-id]
  (when-let [pt (get product-types product-type-id)]
    (>= (:vintage-percent-min pt)
        (vintage-minimum-percent :viticultural-area))))

(defn us-limits-cited?
  "True when every US limit group carries a section and an http(s) provenance."
  []
  (every? (fn [[_ v]]
            (and (string? (:section v))
                 (string? (:provenance v))
                 (str/starts-with? (:provenance v) "http")))
          us-regulatory-limits))

(defn citation-coverage
  "Honest coverage: which figures rest on a fetched source and which do not."
  []
  {:us-limit-groups (count us-regulatory-limits)
   :us-cited? (us-limits-cited?)
   :eu-figures-cited? false
   :styles-meeting-ava-vintage-rule
   (vec (sort (filter style-vintage-minimum-meets-ava-rule? (keys product-types))))
   :note (str "cloud-itonami-isic-1102: all " (count us-regulatory-limits)
              " US limit groups verified against govinfo.gov CFR XML "
              "(4.36(b), 4.21, 4.22(b)(1), 4.27(a), 4.72(a)). EU figures "
              "(Reg (EU) 2019/934, 2019/33) are named for context but NO EU "
              "source was fetched, so they are reported as uncited rather "
              "than given a citation that does not support them. The catalog's "
              "flat 85% vintage minimum is permissive for AVA-labelled wine "
              "(4.27(a)(1) requires 95%); no style satisfies it today, which "
              "style-vintage-minimum-meets-ava-rule? asserts rather than hides.")})

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
