package com.ailudick.capitalismmod.economy.expansion;

import java.util.List;

/** Declarative boundary for one expansion domain. */
public record ExpansionSystemDescriptor(ExpansionSystem system, List<String> coreObjects,
                                        List<ExpansionSystem> dependencies, List<String> outputs) {
    public ExpansionSystemDescriptor {
        if (system == null) throw new IllegalArgumentException("system is required");
        coreObjects = List.copyOf(coreObjects == null ? List.of() : coreObjects);
        dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        outputs = List.copyOf(outputs == null ? List.of() : outputs);
    }
}
