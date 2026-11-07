package DIV.gtcsolo.audit;

import DIV.gtcsolo.integration.jei.multiblock.PreviewFrameLease;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public final class PreviewFrameLeaseTest {
    public static void main(String[] args) { run(); }
    public static void run() {
        ordinaryLifecycle();
        distinctResourcesWithEqualValues();
        ownsResourceUntilRelease();
        boundedAcrossFrames();
        System.out.println("Preview frame leases passed: frame lifecycle, identity, strong ownership and bounded retention.");
    }

    private static void ordinaryLifecycle() {
        var leases = new PreviewFrameLease<Object>();
        var released = new ArrayList<Object>();
        Object first = new Object(), second = new Object();
        leases.beginFrame(); leases.touch(first); leases.expire(released::add);
        assert released.isEmpty();
        // Rechecking a very slow/paused frame must never restart the visible job.
        for (int i=0;i<1000;i++) leases.expire(released::add);
        assert released.isEmpty();
        leases.beginFrame(); leases.touch(second); leases.expire(released::add);
        assert released.size() == 1 && released.get(0) == first;
        leases.beginFrame(); leases.touch(second); leases.expire(released::add);
        assert released.size() == 1;
        leases.beginFrame(); leases.expire(released::add);
        assert released.size() == 2 && released.get(1) == second;
        leases.touch(first); leases.clear(released::add); leases.clear(released::add);
        assert released.size() == 3;
    }

    private static void distinctResourcesWithEqualValues() {
        var leases = new PreviewFrameLease<EqualResource>();
        var released = new ArrayList<EqualResource>();
        EqualResource first = new EqualResource(), second = new EqualResource();
        assert first.equals(second);
        leases.beginFrame();
        leases.touch(first); leases.touch(second); leases.touch(first);
        leases.expire(released::add);
        assert released.isEmpty();
        leases.beginFrame(); leases.touch(second); leases.expire(released::add);
        assert released.size() == 1 && released.get(0) == first;
        leases.clear(released::add);
        assert released.size() == 2 && released.get(1) == second;
    }

    private static void ownsResourceUntilRelease() {
        var leases = new PreviewFrameLease<Object>();
        leases.beginFrame();
        WeakReference<Object> reference = addOnlyOwner(leases);
        System.gc();
        assert reference.get() != null : "The lease must own a live resource until its close callback";
        int[] closed = {0};
        leases.beginFrame(); leases.expire(ignored -> closed[0]++);
        leases.clear(ignored -> closed[0]++);
        assert closed[0] == 1 : "Losing the GUI/cache reference must not skip resource cleanup";
    }

    private static WeakReference<Object> addOnlyOwner(PreviewFrameLease<Object> leases) {
        Object resource = new Object();
        leases.touch(resource);
        return new WeakReference<>(resource);
    }

    private static void boundedAcrossFrames() {
        var leases = new PreviewFrameLease<Object>();
        int[] closed = {0};
        for (int frame = 0; frame < 1000; frame++) {
            leases.beginFrame(); leases.touch(new Object()); leases.expire(ignored -> closed[0]++);
            assert closed[0] == frame : "Only this frame's resource may remain leased";
        }
        leases.clear(ignored -> closed[0]++);
        assert closed[0] == 1000;
        leases.clear(ignored -> closed[0]++);
        assert closed[0] == 1000;
    }

    private static final class EqualResource {
        @Override public boolean equals(Object other) { return other instanceof EqualResource; }
        @Override public int hashCode() { return 1; }
    }
}
