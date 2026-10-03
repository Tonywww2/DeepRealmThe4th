package com.tonywww.deeprealm4th.astral.node;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Logical predicates compose without relying on a shared filler item's physical ID. */
@FunctionalInterface
public interface FillerFilter extends Predicate<CellView> {
    static FillerFilter any() { return cell -> cell.effective(); }
    static FillerFilter item(String id) { return cell -> cell.effective() && id.equals(cell.actualItemId()); }
    static FillerFilter kind(String key) { return cell -> cell.effective() && key.equals(cell.kindKey()); }
    static FillerFilter variant(String key) { return cell -> cell.effective() && key.equals(cell.variantKey()); }
    static FillerFilter rule(String key) { return cell -> cell.effective() && key.equals(cell.ruleKey()); }
    static FillerFilter role(String key) { return cell -> cell.effective() && key.equals(cell.role()); }
    static FillerFilter anyOfRules(Collection<String> keys) {
        Set<String> copy = Set.copyOf(keys);
        return cell -> cell.effective() && copy.contains(cell.ruleKey());
    }
    static FillerFilter where(Predicate<CellView> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate::test;
    }
    default FillerFilter and(FillerFilter other) { return cell -> test(cell) && other.test(cell); }
    default FillerFilter or(FillerFilter other) { return cell -> test(cell) || other.test(cell); }
}
