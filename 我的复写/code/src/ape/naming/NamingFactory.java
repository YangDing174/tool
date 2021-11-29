package ape.naming;

public class NamingFactory {
    private void stateRefinement(List<RefinementResult> results, NamingManager nm, Set<GUITree> affected, Naming currentNaming, StateTransition st1,
                                 StateTransition st2, List<GUITreeTransition> tts1, List<GUITreeTransition> tts2) {
        GUITreeTransition tt1 = tts1.get(tts1.size() - 1);
        GUITreeTransition tt2 = tts2.get(tts2.size() - 1);
        if (isTopNamingEquivalent(tt1.getSource(), tt2.getSource())) {
            Logger.iprintln("Two GUI trees are top naming equivalent..");
            if (isIsomorphic(tt1.getSource(), tt2.getSource())) {
                Logger.iprintln("Two GUI trees are top naming and isomorphic..");
            }
            return;
        }
        State sourceState1 = st1.getSource();
        if (sourceState1 != st2.getSource()) {
            throw new IllegalStateException("Source states should be the same!");
        }

        Set<Name> candidates = new HashSet<>();
        for (ModelAction action : sourceState1.getActions()) {
            if (action.requireTarget()) {
                candidates.add(action.getTarget());
            }
        }
        if (Config.enableReplacingNamelet) {
            if (nm.isLeaf(currentNaming)) {
                Namelet last = currentNaming.getLastNamelet();
                Namer lastNamer = last.getNamer();
                if (currentNaming.isReplaceable(last)) {
                    Namelet parent = last.getParent();
                    Namer parentNamer = parent.getNamer();
                    List<Namer> refinedNamers = NamerFactory.getSortedAbove(parentNamer);
                    List<Namer> upperBounds = new ArrayList<>();
                    outer:
                    for (Namer refined : refinedNamers) {
                        if (!upperBounds.isEmpty()) {
                            for (Namer upper : upperBounds) {
                                if (refined.refinesTo(upper)) {
                                    continue outer; // no retry
                                }
                            }
                        }
                        if (lastNamer.refinesTo(refined)) {
                            continue; // 避免替换为相同的namelet。
                        }
                        Namelet newNamelet = new Namelet(last.getExprString(), refined);
                        Naming newNaming = currentNaming.replaceLast(last, newNamelet);
                        if (!nm.isLeaf(newNaming)) {
                            continue;
                        }
                        if (!checkStateRefinement(newNaming, refined, tts1, tts2, upperBounds)) {
                            continue;
                        }
                        if (!checkPredicate(nm, affected, newNaming)) {
                            continue;
                        }
                        results.add(new RefinementResult(true, currentNaming, newNaming, last, newNamelet, st1, st2, tts1, tts2));
                        break;
                    }
                }
            }
        }
        for (Name name : candidates) {
            String xpathStr = NamerFactory.nameToXPathString(name);
            Namelet currentNamelet = checkNamelet(currentNaming, name, tts1, tts2);
            if (currentNamelet == null) {
                continue;
            }
            Namer currentNamer = name.getNamer();
            List<Namer> refinedNamers = NamerFactory.getSortedAbove(currentNamer);
            List<Namer> upperBounds = new ArrayList<>();
            Set<Namer> visited = new HashSet<Namer>();
            visited.add(currentNamer);
            LinkedList<Namer> queue = new LinkedList<Namer>();
            collectSortedAbove(currentNamer, queue, visited);
            outer: for (Namer refined : refinedNamers) {
                if (!upperBounds.isEmpty()) {
                    for (Namer upper : upperBounds) {
                        if (refined.refinesTo(upper)) {
                            continue outer; // 不进行重试
                        }
                    }
                }
                Namelet newNamelet = new Namelet(xpathStr, refined);
                Naming newNaming = currentNaming.extend(currentNamelet, newNamelet);
                if (!checkStateRefinement(newNaming, refined, tts1, tts2, upperBounds)) {
                    continue;
                }
                if (!checkPredicate(nm, affected, newNaming)) {
                    continue;
                }
                results.add(new RefinementResult(false, currentNaming, newNaming, currentNamelet, newNamelet, st1, st2, tts1, tts2));
                break;
            }
        }
    }

    private void actionRefinement(List<RefinementResult> results, NamingManager nm, Set<GUITree> affected, Naming currentNaming, StateTransition st1,
                                  StateTransition st2, List<GUITreeTransition> tts1, List<GUITreeTransition> tts2) {
        Name widget = st1.getAction().getTarget();
        if (widget == null) {
            return; //对没有目标的动作不进行细化
        }
        if (!isSharedAction(widget, tts1, tts2)) {
            Logger.iprintln("Action is not shared. No action refinement.");
            return;
        }
        Namelet currentNamelet = checkNamelet(currentNaming, widget, tts1, tts2);
        if (currentNamelet == null) {
            return;
        }
        Namer currentNamer = widget.getNamer();
        if (Config.enableReplacingNamelet) {
            if (nm.isLeaf(currentNaming)) {
                if (currentNaming.isReplaceable(currentNamelet)) {
                    Namelet parent = currentNamelet.getParent();
                    Namer parentNamer = parent.getNamer();
                    List<Namer> refinedNamers = NamerFactory.getSortedAbove(parentNamer);
                    List<Namer> upperBounds = new ArrayList<>();
                    outer:
                    for (Namer refined : refinedNamers) {
                        if (!upperBounds.isEmpty()) {
                            for (Namer upper : upperBounds) {
                                if (refined.refinesTo(upper)) {
                                    continue outer; // no retry
                                }
                            }
                        }
                        if (currentNamer.refinesTo(refined)) {
                            continue; // 避免替换为相同的namelet。
                        }
                        Namelet newNamelet = new Namelet(currentNamelet.getExprString(), refined);
                        Naming newNaming = currentNaming.replaceLast(currentNamelet, newNamelet);
                        if (!nm.isLeaf(newNaming)) {
                            continue;
                        }
                        if (checkActionRefinement(newNaming, refined, tts1, tts2, upperBounds) == false) {
                            continue outer;
                        }
                        if (!checkPredicate(nm, affected, newNaming)) {
                            continue;
                        }
                        results.add(new RefinementResult(true, currentNaming, newNaming, currentNamelet, newNamelet, st1, st2, tts1, tts2));
                        break;
                    }
                }
            }
        }
        List<Namer> refinedNamers = NamerFactory.getSortedAbove(currentNamer);
        List<Namer> upperBounds = new ArrayList<>();
        outer: for (Namer refined : refinedNamers) {
            if (!upperBounds.isEmpty()) {
                for (Namer upper : upperBounds) {
                    if (refined.refinesTo(upper)) {
                        continue outer; // 不进行重试
                    }
                }
            }
            String xpathStr = NamerFactory.nameToXPathString(widget);
            Namelet newNamelet = new Namelet(xpathStr, refined);
            Naming newNaming = currentNaming.extend(currentNamelet, newNamelet);
            if (checkActionRefinement(newNaming, refined, tts1, tts2, upperBounds) == false) {
                continue;
            }
            if (!checkPredicate(nm, affected, newNaming)) {
                continue;
            }
            results.add(new RefinementResult(true, currentNaming, newNaming, currentNamelet, newNamelet, st1, st2, tts1, tts2));
            break;
        }
    }
}
