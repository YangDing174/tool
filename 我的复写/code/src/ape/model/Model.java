package ape.model;

public class Model {
    static enum ModelEvent {
        NON_DETERMINISTIC_TRANSITION,
        ACTION_REFINEMENT,
        STATE_ABSTRACTION,
    }

    public Model actionRefinement(ModelAction action) {
        int version = this.version;
        long begin = SystemClock.elapsedRealtimeNanos();
        namingManager.actionRefinement(this, action);
        long end = SystemClock.elapsedRealtimeNanos();
        if (version == this.version) {
            return null;
        }
        eventCounters.logEvent(ModelEvent.ACTION_REFINEMENT);
        Logger.iformat("Action refinement takes %s ms.", TimeUnit.NANOSECONDS.toMillis(end - begin));
        return this;
    }
}
