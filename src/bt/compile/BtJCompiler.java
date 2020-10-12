package bt.compile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import bt.compile.obj.BtJClass;
import bt.console.output.ConsoleTable;
import bt.runtime.InstanceKiller;
import bt.types.Killable;
import bt.utils.Exceptions;
import bt.utils.FileUtils;
import bt.utils.Null;

/**
 * @author &#8904
 *
 */
public class BtJCompiler implements Killable
{
    private List<String> codeLines = new ArrayList<>();
    private Scanner input;

    private void init()
    {
        InstanceKiller.killOnShutdown(this);

        System.out.println("\r\n===========================================================\r\n"
                              + "______ _     ___ _____                       _ _           \r\n"
                              + "| ___ \\ |   |_  /  __ \\                     (_) |          \r\n"
                              + "| |_/ / |_    | | /  \\/ ___  _ __ ___  _ __  _| | ___ _ __ \r\n"
                              + "| ___ \\ __|   | | |    / _ \\| '_ ` _ \\| '_ \\| | |/ _ \\ '__|\r\n"
                              + "| |_/ / |_/\\__/ / \\__/\\ (_) | | | | | | |_) | | |  __/ |   \r\n"
                              + "\\____/ \\__\\____/ \\____/\\___/|_| |_| |_| .__/|_|_|\\___|_|   \r\n"
                              + "                                      | |                  \r\n"
                              + "                                      |_|                  \r\n"
                              + "===========================================================");
    }

    public BtJCompiler()
    {
        init();
        this.codeLines = new ArrayList<>();
        this.input = new Scanner(System.in);

        startUserInput();
    }

    public BtJCompiler(String filePath)
    {
        init();

        try
        {
            BtJClass clazz = getClassFromFile(filePath);
            Path classPath = compile(clazz);
            run(classPath, clazz);
        }
        catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
               | SecurityException e)
        {
            e.printStackTrace();
        }
    }

    private void startUserInput()
    {
        boolean continueToRun = true;

        while (continueToRun)
        {
            continueToRun = handleInput(this.input.nextLine());
        }
    }

    private boolean handleInput(String input)
    {
        boolean continueToRun = true;
        input = input.trim();

        if (input.equalsIgnoreCase("run"))
        {
            try
            {
                BtJClass clazz = generateClassFromCodeLines();
                Path classPath = compile(clazz);
                run(classPath, clazz);
            }
            catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                   | SecurityException e)
            {
                e.printStackTrace();
            }
        }
        else if (input.equalsIgnoreCase("exit"))
        {
            continueToRun = false;
        }
        else if (input.equalsIgnoreCase("show"))
        {
            var table = new ConsoleTable(5, 100);

            for (int i = 0; i < this.codeLines.size(); i ++ )
            {
                table.addRow(i, this.codeLines.get(i));
            }

            System.out.println("\r\n" + table);
        }
        else if (input.equalsIgnoreCase("clear"))
        {
            this.codeLines.clear();
        }
        else if (input.toLowerCase().startsWith("clear "))
        {
            String[] parts = input.split(" ");

            try
            {
                int line = Integer.parseInt(parts[1]);
                this.codeLines.remove(line);
            }
            catch (NumberFormatException e)
            {
                System.err.println("Invalid line number.");
            }
        }
        else if (input.equalsIgnoreCase("help"))
        {
            System.out.println("Available commands:");
            System.out.println(" run = execute the stored code as a whole");
            System.out.println(" exit = exit the BtJCompiler");
            System.out.println(" show = show all currently stored lines of code");
            System.out.println(" clear = clear all currently stored lines of code");
            System.out.println(" clear <n> = clear code in line n");
        }
        else
        {
            this.codeLines.add(input);
        }

        return continueToRun;
    }

    private BtJClass generateClassFromCodeLines() throws FileNotFoundException, IOException
    {
        String snippet = "";

        for (String line : this.codeLines)
        {
            snippet += line;
        }

        String code = FileUtils.readFile(FileUtils.getJarDirectory(getClass()).getAbsolutePath() + "/class_template.java");

        code = code.replace("<source>", snippet);

        return new BtJClass("BtJCompiler_source", code);
    }

    private BtJClass getClassFromFile(String filePath) throws IOException
    {
        System.out.println("Reading file " + filePath);
        File file = new File(filePath);

        String className = file.getName();
        className = className.substring(0, className.lastIndexOf('.'));

        InputStream stream = new FileInputStream(filePath);
        String separator = System.getProperty("line.separator");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String code = reader.lines().collect(Collectors.joining(separator));
        reader.close();

        return new BtJClass(className, code);
    }

    private Path compile(BtJClass clazz) throws UnsupportedEncodingException, IOException
    {
        System.out.println("Compiling " + clazz.getClassName());

        String tmpProperty = System.getProperty("java.io.tmpdir");
        Path sourcePath = Paths.get(tmpProperty, clazz.getClassName() + ".java");
        Files.write(sourcePath, clazz.getCode().getBytes("UTF-8"));

        System.out.println("Saved source to " + sourcePath.toString());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourcePath.toFile().getAbsolutePath());
        Path classPath = sourcePath.getParent().resolve(clazz.getClassName() + ".class");

        System.out.println("Compiled class file " + classPath.toString());

        return classPath;
    }

    private void run(Path classPath, BtJClass clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                                                     NoSuchMethodException, SecurityException, IOException
    {
        System.out.println("Running " + clazz.getClassName());
        System.out.println("===========================================================");

        URL classUrl = classPath.getParent().toFile().toURI().toURL();

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]
        {
          classUrl
        });

        Class<?> cls = Class.forName(clazz.getClassName(), true, classLoader);
        cls.getConstructor().newInstance();

        classLoader.close();

        System.out.println("===========================================================");
        System.out.println("Done");
    }

    /**
     * @see bt.types.Killable#kill()
     */
    @Override
    public void kill()
    {
        System.out.println("Exiting BtJCompiler");

        Exceptions.logThrow(() -> Null.checkClose(this.input));
    }
}