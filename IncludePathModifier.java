import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class IncludePathModifier {
    //当前程序工作路径，请根据自己的文件夹做出对应修改
    static String rootPath = System.getProperty("user.dir") + "/Resource";
    static int rootPathLength = rootPath.length();

    //需要替换的include行开头
    static String startInclude = "#include \"";
    //举例：对于 #include "asm/assembler.hpp" 来说，head = asm，newHead = ./src/share/vm/asm
    //我们最后将 #include "asm/assembler.hpp" 变成了 #include "./src/share/vm/asm/assembler.hpp"
    //这样就把路径补全了
    static String head = null;
    static String newHead = null;

    /**
     * 补齐include包含文件路径
     */
    public static void modifyIncludePath() {
        getFileListsAndFolderSets(new File(rootPath));
        fileLists.forEach(f -> loadFileByLine(f));
    }

    static List<String> fileLists = new ArrayList<>();
    static Set<String> folderSets = new TreeSet<>();
    /**
     * 1.递归获取.hpp .cpp .c .h等文件绝对路径，存在List中
     * 2.同时获取其文件夹目录，存在Set中（Set是为了相同的只存一次）
     * @param currentPath
     */
    public static void getFileListsAndFolderSets(File currentPath) {
        for (File tmp : currentPath.listFiles()) {
            if (tmp.isDirectory()) {
                getFileListsAndFolderSets(tmp);
            }
            if (tmp.isFile() && filterFileByExt(tmp.getName())) {
                fileLists.add(tmp.getAbsolutePath());
                folderSets.add("." + tmp.getParent().substring(rootPathLength));
            }
        }
    }

    /**
     * 过滤文件，只留下.hpp .cpp .c .h
     * @param fileName
     * @return
     */
    public static boolean filterFileByExt(String fileName) {
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        if ("hpp".equals(suffix) || "cpp".equals(suffix)
            || "c".equals(suffix) || "h".equals(suffix)) {
            return true;
        }
        return false;
    }

    /**
     * 逐行处理，利用newLines作为缓冲，替换后再写入源文件
     * @param fileAbsolutePath
     */
    public static void loadFileByLine(String fileAbsolutePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileAbsolutePath));
            List<String> newLines = new ArrayList<>();

            lines.stream()
                .forEach(l -> {
                    if (l.startsWith(startInclude) && l.contains("/")) {
                        head = l.substring(startInclude.length(), l.indexOf("/"));
                        List<String> tmp = folderSets.stream()
                            .filter(st -> st.contains("/"))
                            .filter(st -> head.equals(st.substring(st.lastIndexOf("/") + 1)))
                            .collect(Collectors.toList());
                        if (tmp.size() > 0) {
                            newHead = tmp.get(0);
                            newLines.add(l.replace(startInclude + head, startInclude + newHead));
                        } else {
                            newLines.add(l);
                        }
                    } else {
                        newLines.add(l);
                    }
                });
            Files.write(Paths.get(fileAbsolutePath),newLines);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) {
        modifyIncludePath();
    }
}
