package nora.compiler.entries.interfaces;

import nora.compiler.entries.Definition;
import nora.compiler.resolver.PathTreeNode;

public interface Module extends Definition {

    String getName();

    PathTreeNode getNode();
}
