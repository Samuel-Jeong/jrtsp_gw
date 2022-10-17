package org.kkukie.jrtsp_gw.media.bouncycastle.util.test;

public interface TestResult
{
    public boolean isSuccessful();
    
    public Throwable getException();
    
    public String toString();
}
