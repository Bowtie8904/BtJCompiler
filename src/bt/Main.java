package bt;

import bt.compile.BtJCompiler;
import bt.console.input.ArgumentParser;
import bt.console.input.ValueArgument;

/**
 * @author &#8904
 *
 */
public class Main
{
    public static void main(String[] args)
    {
        var parser = new ArgumentParser("-");
        var fileArgument = new ValueArgument("f", "file").usage("-f <file path>")
                                                         .description("[Optional] Attempts to compile and run the given .java file.");
        parser.registerDefaultHelpArgument("h", "help");
        parser.register(fileArgument);
        parser.parse(args);

        if (!parser.wasExecuted("h"))
        {
            if (fileArgument.isExecuted())
            {
                new BtJCompiler(fileArgument.getValue());
            }
            else
            {
                new BtJCompiler();
            }
        }
    }
}