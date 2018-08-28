package com.p6majo.logger;

public class Logger {

    public static enum Level {info,warning,debug,error}
    private boolean isDebugEnabled = true;


    private Class callingClass;
    public Logger(Class callingClass){
        this.callingClass = callingClass;
    }

    public static void logging(Level level,String message){
        switch(level){
            case info:
                System.out.println("info: " + message);
                break;
            case warning:
                System.err.println("warning: " + message);
                break;
            case debug:
                System.out.println("debug: " + message);
                break;
            case error:
                System.err.println("error: "+message);
                System.exit(0);
                break;
        }

    }


    public void log(Level level,String message){
        switch(level){
            case info:
                System.out.println("info in "+callingClass.getName()+": " +message);
                break;
            case warning:
                System.err.println("warning in " +callingClass.getName()+": " + message);
                break;
            case debug:System.out.println("debug: " + message);
                break;
            case error:
                System.err.println("error in "+callingClass.getName()+": " +message);
                System.exit(0);
                break;
        }

    }

    public boolean isDebugEnabled() {
        return isDebugEnabled;
    }

}

