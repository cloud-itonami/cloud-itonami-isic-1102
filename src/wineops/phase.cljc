(ns wineops.phase
  "Phase machine: the states a wine production batch transits through.

  State machine:
    :intake -> :crush -> :fermentation -> :pressing -> :aging ->
    :bottling -> :audit -> :archived

  `:intake` is grape receiving; `:crush` is crush/destem (and, for red
  wine, the start of skin contact); `:fermentation` is primary (and, for
  some styles, secondary/malolactic) fermentation -- this is where ABV
  develops and where SO2 additions typically begin; `:pressing` separates
  free-run/press wine from skins and lees; `:aging` is barrel/tank
  maturation and blending; `:bottling` is finished-product bottling
  (never directly controlled by this actor -- fermentation-tank and
  bottling-line equipment operation remain exclusive to winery staff);
  `:audit` is compliance audit; `:archived` is the terminal state.

  Each transition can accept a proposal and yield an audit fact.")

(def all-phases
  "All valid phases in the wine production workflow."
  [:intake :crush :fermentation :pressing :aging :bottling :audit :archived])

(def phase-sequence
  "Ordered phases representing normal batch progression."
  [:intake :crush :fermentation :pressing :aging :bottling :audit :archived])

(defn valid-phase?
  "Check if a phase is valid."
  [phase]
  (contains? (set all-phases) phase))

(defn- index-of
  "Portable (Clojure/ClojureScript) index lookup -- `.indexOf` is a
  JVM-only `java.util.List` method that ClojureScript's PersistentVector
  does not implement, so it is avoided here even though `phase-sequence`
  is a plain vector. Returns -1 when `x` is not found, matching
  `java.util.List/indexOf`'s contract."
  [coll x]
  (or (first (keep-indexed (fn [i v] (when (= v x) i)) coll)) -1))

(defn can-transition?
  "Check if a transition from one phase to another is valid
  (must be forward-only in the sequence, no backtracking). Always returns a
  boolean (never nil), including when either phase is invalid."
  [from-phase to-phase]
  (boolean
   (and (valid-phase? from-phase) (valid-phase? to-phase)
        (let [from-idx (index-of phase-sequence from-phase)
              to-idx (index-of phase-sequence to-phase)]
          (and (>= from-idx 0) (>= to-idx 0) (< from-idx to-idx))))))
