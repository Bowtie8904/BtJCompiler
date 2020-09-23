package bt.compile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import bt.console.ConsoleTable;
import bt.log.Logger;
import bt.runtime.InstanceKiller;
import bt.types.Killable;
import bt.utils.exc.Exceptions;
import bt.utils.nulls.Null;

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
        Logger.global().setPrintCaller(false);
        Logger.global().setLogToFile(false);
        Logger.global().hookSystemOut();
        Logger.global().hookSystemErr();

        Logger.global().print("\r\n===========================================================\r\n"
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
            Logger.global().print(e);
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
                Logger.global().print(e);
            }
        }
        else if (input.equalsIgnoreCase("exit"))
        {
            continueToRun = false;
        }
        else if (input.equalsIgnoreCase("show"))
        {
            var table = new ConsoleTable(100);

            for (String line : this.codeLines)
            {
                table.addRow(line);
            }

            Logger.global().print("\r\n" + table);
        }
        else if (input.equalsIgnoreCase("clear"))
        {
            this.codeLines.clear();
        }
        else if (input.equalsIgnoreCase("help"))
        {
            Logger.global().print("Available commands:");
            Logger.global().print(" run = execute the stored code as a whole");
            Logger.global().print(" exit = exit the BtJCompiler");
            Logger.global().print(" show = show all currently stored lines of code");
            Logger.global().print(" clear = clear all currently stored lines of code");
        }
        else
        {
            this.codeLines.add(input);
        }

        return continueToRun;
    }

    private BtJClass generateClassFromCodeLines()
    {
        String snippet = "";

        for (String line : this.codeLines)
        {
            snippet += line;
        }

        String code = "import bt.log.Logger;"
                      + "import java.util.*;"
                      + "public class BtJCompiler_source\r\n"
                      + "{\r\n"
                      + "    public BtJCompiler_source()\r\n"
                      + "    {\r\n"
                      + "         try\r\n"
                      + "         {\r\n"
                      + "             execute();\r\n"
                      + "         }\r\n"
                      + "         catch (Exception e)\r\n"
                      + "         {\r\n"
                      + "              Logger.global().print(e);\r\n"
                      + "         }\r\n"
                      + "    }\r\n"
                      + "\r\n"
                      + "    private void execute() throws Exception\r\n"
                      + "    {\r\n"
                      + "        <source>\r\n"
                      + "    }\r\n"
                      + "\r\n"
                      + "    private void log(Object o)\r\n"
                      + "    {\r\n"
                      + "        Logger.global().print(o);\r\n"
                      + "    }\r\n"
                      + "}";
        code = code.replace("<source>", snippet);

        return new BtJClass("BtJCompiler_source", code);
    }

    private BtJClass getClassFromFile(String filePath) throws IOException
    {
        Logger.global().print("Reading file " + filePath);
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
        Logger.global().print("Compiling " + clazz.getClassName());

        String tmpProperty = System.getProperty("java.io.tmpdir");
        Path sourcePath = Paths.get(tmpProperty, clazz.getClassName() + ".java");
        Files.write(sourcePath, clazz.getCode().getBytes("UTF-8"));

        Logger.global().print("Saved source to " + sourcePath.toString());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourcePath.toFile().getAbsolutePath());
        Path classPath = sourcePath.getParent().resolve(clazz.getClassName() + ".class");

        Logger.global().print("Compiled class file " + classPath.toString());

        return classPath;
    }

    private void run(Path classPath, BtJClass clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                                                     NoSuchMethodException, SecurityException, IOException
    {
        Logger.global().print("Running " + clazz.getClassName());
        Logger.global().print("===========================================================");

        URL classUrl = classPath.getParent().toFile().toURI().toURL();

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]
        {
          classUrl
        });

        Class<?> cls = Class.forName(clazz.getClassName(), true, classLoader);
        cls.getConstructor().newInstance();

        classLoader.close();

        Logger.global().print("===========================================================");
        Logger.global().print("Done");
    }

    /**
     * @see bt.types.Killable#kill()
     */
    @Override
    public void kill()
    {
        Logger.global().print("Exiting BtJCompiler");

        Exceptions.logThrow(() -> Null.checkClose(this.input));
    }
}