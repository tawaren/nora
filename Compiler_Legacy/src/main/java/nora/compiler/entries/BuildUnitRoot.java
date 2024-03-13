package nora.compiler.entries;

import nora.compiler.resolver.PathTreeNode;

public interface BuildUnitRoot {
    Definition resolve(PathTreeNode root);
    boolean validateAndInfer();
}
