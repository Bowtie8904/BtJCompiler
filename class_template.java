import bt.log.Logger;
import bt.utils;
import java.util.*;

public class BtJCompiler_source
{
    public BtJCompiler_source()
    {
        try
        {
            execute();
        }
        catch (Exception e)
        {
            Logger.global().print(e);
        }
    }

    private void execute() throws Exception
    {
        <source>
    }

    private void log(Object o)
    {
        Logger.global().log(o);
    }
}