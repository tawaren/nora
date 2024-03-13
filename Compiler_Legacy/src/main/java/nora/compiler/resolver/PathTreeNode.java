package nora.compiler.resolver;

import nora.compiler.entries.Definition;
import nora.compiler.entries.BuildUnitRoot;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class PathTreeNode {
    private final String name;
    private final Definition def;
    private Map<String, PathTreeNode> children = null;

    public PathTreeNode(String name, Definition def) {
        this.name = name;
        this.def = def;
    }

    public PathTreeNode addAndDescend(String name, Definition def){
        if(children==null) children = new HashMap<>();
        var res = children.computeIfAbsent(name, n -> new PathTreeNode(n, def));
        if(res.def != def) throw new RuntimeException("Definition already exists as "+def);
        return res;
    }

    public String getName() {
        return name;
    }

    public Definition getEntry() {
        return def;
    }

    public PathTreeNode navigatePath(String[] path){
        var cur = this;
        for(String elem:path){
            var child = cur.children;
            if(child == null) return null;
            cur = child.get(elem);
            if(cur == null) return null;
        }
        return cur;
    }

    public PathTreeNode navigatePath(String path){
        return navigatePath(path.split("\\."));
    }

    public Set<String> listEntries() {
        return children.keySet();
    }

    public PathTreeNode getChild(String name){
        if(children == null) return null;
        return children.get(name);
    }

    public void forEach(Function<Definition, Boolean> f){
        if(f.apply(getEntry())){
            if(children == null) return;
            for(PathTreeNode kid: children.values()){
                kid.forEach(f);
            }
        }
    }

    public void buildString(String path, StringBuilder builder){
        var nPath = path;
        if(nPath.isBlank()){
            nPath = name;
        } else {
            nPath = nPath+"."+name;
        }

        builder.append(nPath).append(": ").append(def).append("\n");
        if(children == null)return;
        for(PathTreeNode c:children.values()){
            c.buildString(nPath,builder);
        }
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        buildString("",builder);
        return builder.toString();
    }


}
