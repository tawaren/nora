package nora.compiler.resolver;

import nora.compiler.entries.Definition;
import nora.compiler.entries.resolved.Generic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportResolver {
    private final Map<String, PathTreeNode> imports = new HashMap<>();
    private final PathTreeNode root;

    public ImportResolver(PathTreeNode root) {
        this.root = root;
    }

    public void addImport(String imp) {
        if(imp.endsWith(".*")){
            var nImp = imp.substring(0, imp.length()-2);
            var res = root.navigatePath(nImp);
            if(res == null) throw new RuntimeException("Can not find import: "+nImp);
            for(String entry:res.listEntries()){
                addDefinition(res.getChild(entry));
            }
            imports.putIfAbsent(res.getName(), res);
        } else {
           var res = root.navigatePath(imp);
           if(res == null) throw new RuntimeException("Can not find import: "+imp);
            addDefinition(res);
        }
    }

    public void addDefinition(PathTreeNode def){
        if(imports.containsKey(def.getName())) {
            System.out.println("Import Shadowing Warning: "+def.getName());
        }
        imports.put(def.getName(), def);
    }

    Definition resolveReference(String relativePath){
        var elems = relativePath.split("\\.");
        var start = imports.get(elems[0]);
        if (start == null) start = root.getChild(elems[0]);
        if (start == null) throw new RuntimeException("Can not find definition: "+relativePath);
        if (elems.length == 1) return start.getEntry();
        var restPath = new String[elems.length-1];
        System.arraycopy(elems,1,restPath,0, elems.length-1);
        var end = start.navigatePath(restPath);
        if (end == null) throw new RuntimeException("Can not find definition: "+relativePath);
        return end.getEntry();
    }
}
