# cloud-itonami-isic-1102: Wine Manufacturing Coordination Actor

**ISIC Rev. 4 1102** — Manufacture of Wines

A distributed actor for autonomous, compliant coordination of wine-manufacturing plant operations: grape intake → crush/destem → fermentation (primary and, for some styles, malolactic) → pressing → aging/blending → bottling → ABV/residual-sugar/volatile-acidity/SO2-residue/fill-volume/vintage-percent inspection → sulfite labeling → finished-product logistics. Sealed LLM advisor; independent Governor enforcement; append-only audit ledger. **Not equipment control.** Fermentation-tank and bottling-line equipment operation remain exclusive to licensed winery staff, and excise/tax-classification decisions (e.g. whether a batch's actual ABV crosses into a different federal/state tax class) remain exclusive to human operators and tax authorities.

## Scope

This actor coordinates **plant-operations workflow** for wine manufacturing (still table wine, sparkling wine, dessert/sweet wine, fortified wine):
- Production batch logging (grape intake, fermentation/bottling parameters, ABV, vintage data, evidence checklist)
- Equipment maintenance scheduling (fermentation tanks, presses, bottling lines, fill-volume meters)
- Food-safety concern escalation (sulfur-dioxide residue, contamination, excess ABV for the declared class)
- Finished-product shipment coordination

**Out of scope:**
- Direct fermentation/bottling-line equipment control (winery staff exclusive)
- Excise/tax-classification authority (human/tax-authority only — this actor never reclassifies a batch's tax category, it only logs observed values and, when warranted, raises a flag)
- Food-safety certification authority (human inspector/regulator only)
- Regulatory interpretation (proposals cite jurisdiction specifications; the Governor enforces only published requirements)
- Any actual sale/distribution transaction (this actor performs coordination and compliance-logging only, never executes a sale)

## Design

### Governor (Independent Compliance Layer)

The Governor is the separation-of-powers enforcement. It never trusts the advisor's confidence for anything safety- or compliance-relevant, and it always wins over the advisor.

- **Hard HOLD** (un-overridable):
  - Operation outside the closed allowlist (`:op-not-allowed`) — includes any proposal that would touch fermentation/bottling-line control or excise/tax-classification-authority decisions
  - Proposal asserting an `:effect` other than `:propose` (`:effect-not-propose`)
  - Winery/batch record not independently verified/registered before any proposal is made against it (`:batch-not-registered`) — applies to every proposal op, not only shipment coordination
  - No jurisdiction citation (`:no-spec-basis`) — can't verify requirements without one
  - Evidence checklist incomplete (`:evidence-incomplete`)
  - ABV outside the declared product's tolerance band (`:abv-out-of-tolerance`) — per 27 CFR 4.36, crossing the band risks a federal excise-tax-class misclassification, a decision this actor never makes
  - Residual sugar outside the declared style's window (`:residual-sugar-out-of-range`)
  - Volatile acidity exceeds the product's spoilage/quality ceiling (`:volatile-acidity-exceeds-max`)
  - Sulfur-dioxide (SO2) residue exceeds the product's regulatory action level (`:so2-residue-exceeded`)
  - Same-vintage-year percentage below the minimum for a vintage-date label claim (`:vintage-percent-below-minimum`) — the "85% rule", 27 CFR 4.27 / EU Reg (EU) 2019/33 Art. 51
  - Contamination detected on the batch's own inspection — foreign material / cork-taint (TCA) / spoilage marker (`:contamination-detected`)
  - Bottling-line fill-volume metering calibration overdue (`:bottling-line-calibration-overdue`)
  - Finished-product fill-volume variance excessive (`:fill-volume-variance-excessive`) — standard-of-fill, 27 CFR 4.72
  - Sulfite labeling mismatch — SO2 residue crosses the jurisdiction's declaration threshold without a `:sulfites` declaration (`:sulfite-label-mismatch`)
  - Winery/cellar sanitation score insufficient (`:sanitation-score-insufficient`)
  - Unresolved food-safety flag (`:food-safety-flag-unresolved`)
  - Batch already processed / shipment already finalized (double-commit guards)
- **Escalate** (human sign-off always required):
  - `:log-production-batch` / `:coordinate-shipment` — real actuation events, always require winery-operator sign-off even when the Governor is otherwise clean
  - `:flag-food-safety-concern` — a food-safety concern (SO2 residue, contamination, excess ABV for the declared class) is never auto-resolved by advisor confidence alone
  - Low advisor confidence (below `governor/confidence-floor`, 0.6)
- **Commit** (advisor proposal approved; Governor clean; not a mandatory-escalation op):
  - Routine, low-stakes proposals only — in this actor's current allowlist that is effectively `:schedule-maintenance` when clean

### Operations (Proposals)

Closed allowlist — the advisor may **only** ever propose these four operation types, all `:effect :propose`:

- **`:log-production-batch`** — Log grape-intake → crush → fermentation → pressing → aging → bottling batch (ABV, vintage data) into production records (always requires human sign-off)
- **`:schedule-maintenance`** — Propose equipment maintenance for fermentation tanks/presses/bottling lines/fill-volume meters (routine, low risk)
- **`:flag-food-safety-concern`** — Surface a food-safety or compliance concern (e.g. SO2 residue, contamination, sulfite-labeling risk, excess ABV for the declared class); always escalates
- **`:coordinate-shipment`** — Coordinate outbound shipment of finished product (always requires human sign-off)

Any proposal for an operation outside this allowlist — most importantly anything that would amount to direct fermentation/bottling-line control, or an excise/tax-classification-authority decision — is refused unconditionally by the Governor (`:op-not-allowed`), regardless of advisor confidence.

## Testing

```bash
# Run full test suite
clojure -M:test

# Check code quality
clojure -M:lint

# Run demo simulation
clojure -M:run
```

## Standalone Use

This repo is **forkable outside the workspace**. If cloning standalone (not in the kotoba-lang monorepo), override `:local/root` paths in `deps.edn`:

```clojure
{:deps {io.github.kotoba-lang/langchain {:git/url "https://github.com/kotoba-lang/langchain" :git/tag "v0.1.0"}
        io.github.kotoba-lang/langgraph {:git/url "https://github.com/kotoba-lang/langgraph" :git/tag "v0.1.0"}}}
```

## License

AGPL-3.0-or-later. Forking/contribution welcome; see `CONTRIBUTING.md`.

## Security

Report security issues to the issue tracker or private disclosure; see `SECURITY.md`.

---

Part of **cloud-itonami**: autonomous actor fleet for regulated industries. See [github.com/cloud-itonami](https://github.com/cloud-itonami).
