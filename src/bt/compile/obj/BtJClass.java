package bt.compile.obj;

/**
 * @author &#8904
 *
 */
public class BtJClass
{
    private String className;
    private String code;

    public BtJClass(String className, String code)
    {
        this.className = className;
        this.code = code;
    }

    /**
     * @return the className
     */
    public String getClassName()
    {
        return this.className;
    }

    /**
     * @param className
     *            the className to set
     */
    public void setClassName(String className)
    {
        this.className = className;
    }

    /**
     * @return the code
     */
    public String getCode()
    {
        return this.code;
    }

    /**
     * @param code
     *            the code to set
     */
    public void setCode(String code)
    {
        this.code = code;
    }

}