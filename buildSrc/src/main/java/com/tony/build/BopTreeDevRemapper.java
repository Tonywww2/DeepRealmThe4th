package com.tony.build;

import net.fabricmc.loom.api.remapping.RemapperExtension;
import net.fabricmc.loom.api.remapping.RemapperParameters;
import net.fabricmc.loom.api.remapping.RemapperContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Dev-only Loom bridge for BOP 19.0.0.96's AT-opened private TreeFeature method.
 * Kept in buildSrc because Loom loads extensions by class name from its plugin classloader. */
public final class BopTreeDevRemapper implements RemapperExtension<RemapperParameters.None> {
    public BopTreeDevRemapper() {}
    @Override public ClassVisitor insertVisitor(String className,RemapperContext context,ClassVisitor next) {
        if(!context.sourceNamespace().equals("srg")||!context.targetNamespace().equals("named")
                ||!className.startsWith("biomesoplenty/worldgen/feature/tree/"))return next;
        return new ClassVisitor(Opcodes.ASM9,next) {
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                String mapped=name.equals("m_225257_")?context.remapper().mapMethodName(
                        "net/minecraft/world/level/levelgen/feature/TreeFeature",name,descriptor):name;
                return super.visitMethod(access,mapped,descriptor,signature,exceptions);
            }
        };
    }
}
