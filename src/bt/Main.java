package bt;

import bt.compile.BtJCompiler;

/**
 * @author &#8904
 *
 */
public class Main
{
    public static void main(String[] args)
    {
        if (args.length == 0)
        {
            new BtJCompiler();
        }
        else
        {
            new BtJCompiler(args[0]);
        }
    }
}