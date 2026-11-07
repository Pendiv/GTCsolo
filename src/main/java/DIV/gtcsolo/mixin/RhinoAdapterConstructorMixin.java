package DIV.gtcsolo.mixin;

import dev.latvian.mods.rhino.JavaAdapter;
import dev.latvian.mods.rhino.classfile.ClassFileWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Repairs obsolete constructor and callback targets emitted by Rhino build.18. */
@Mixin(value = JavaAdapter.class, remap = false)
public abstract class RhinoAdapterConstructorMixin {
    @Redirect(method = "generateCtor", at = @At(value = "INVOKE",
            target = "Ldev/latvian/mods/rhino/classfile/ClassFileWriter;addInvoke(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
            require = 1)
    private static void gtcsolo$wrapperContext(ClassFileWriter writer, int opcode,
                                               String owner, String name, String descriptor) {
        if (owner.equals("dev/latvian/mods/rhino/JavaAdapter") && name.equals("createAdapterWrapper")
                && descriptor.equals("(Ldev/latvian/mods/rhino/Scriptable;Ljava/lang/Object;)Ldev/latvian/mods/rhino/Scriptable;")) {
            // Generated ctor slots: 0=this, 1=delegee, 2=Context, 3+=super arguments.
            writer.addALoad(2);
            descriptor = "(Ldev/latvian/mods/rhino/Scriptable;Ljava/lang/Object;Ldev/latvian/mods/rhino/Context;)Ldev/latvian/mods/rhino/Scriptable;";
        }
        writer.addInvoke(opcode, owner, name, descriptor);
    }

    @Redirect(method = "generateMethod", at = @At(value = "INVOKE",
            target = "Ldev/latvian/mods/rhino/classfile/ClassFileWriter;addInvoke(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
            require = 1)
    private static void gtcsolo$callbackContext(ClassFileWriter writer, int opcode,
                                                String owner, String name, String descriptor) {
        if (owner.equals("dev/latvian/mods/rhino/JavaAdapter")) {
            if (name.equals("getFunction") && descriptor.equals(
                    "(Ldev/latvian/mods/rhino/Scriptable;Ljava/lang/String;)Ldev/latvian/mods/rhino/Function;")) {
                writer.addALoad(0);
                writer.add(180, writer.getClassName(), "context", "Ldev/latvian/mods/rhino/Context;");
                owner = "DIV/gtcsolo/integration/kubejs/RhinoAdapterCallbacks";
                descriptor = "(Ldev/latvian/mods/rhino/Scriptable;Ljava/lang/String;Ldev/latvian/mods/rhino/Context;)Ldev/latvian/mods/rhino/Function;";
            } else if (name.equals("callMethod") && descriptor.equals(
                    "(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Scriptable;Ldev/latvian/mods/rhino/Function;[Ljava/lang/Object;J)Ljava/lang/Object;")) {
                owner = "DIV/gtcsolo/integration/kubejs/RhinoAdapterCallbacks";
            }
        }
        writer.addInvoke(opcode, owner, name, descriptor);
    }
}
