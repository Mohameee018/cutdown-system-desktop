package Clothes_system.cloud;

import java.util.*;

final class MiniJson {
    private final String s; private int p;
    private MiniJson(String s){this.s=s;}
    static Object parse(String s){return new MiniJson(s).value();}
    private void ws(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}
    private Object value(){ws();if(p>=s.length())throw err("end");char c=s.charAt(p);if(c=='{')return object();if(c=='[')return array();if(c=='"')return string();if(take("true"))return Boolean.TRUE;if(take("false"))return Boolean.FALSE;if(take("null"))return null;return number();}
    private Map<String,Object> object(){expect('{');Map<String,Object>m=new LinkedHashMap<>();ws();if(peek('}')){p++;return m;}while(true){String k=string();ws();expect(':');m.put(k,value());ws();if(peek('}')){p++;return m;}expect(',');}}
    private List<Object> array(){expect('[');List<Object>a=new ArrayList<>();ws();if(peek(']')){p++;return a;}while(true){a.add(value());ws();if(peek(']')){p++;return a;}expect(',');}}
    private String string(){expect('"');StringBuilder b=new StringBuilder();while(p<s.length()){char c=s.charAt(p++);if(c=='"')return b.toString();if(c!='\\'){b.append(c);continue;}if(p>=s.length())throw err("escape");c=s.charAt(p++);switch(c){case '"','\\','/'->b.append(c);case 'b'->b.append('\b');case 'f'->b.append('\f');case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'u'->{if(p+4>s.length())throw err("unicode");b.append((char)Integer.parseInt(s.substring(p,p+4),16));p+=4;}default->throw err("escape");}}throw err("string");}
    private Number number(){int st=p;if(peek('-'))p++;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;if(peek('.')){p++;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;}if(peek('e')||peek('E')){p++;if(peek('+')||peek('-'))p++;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;}String n=s.substring(st,p);try{return n.matches(".*[.eE].*")?Double.parseDouble(n):Long.parseLong(n);}catch(Exception e){throw err("number");}}
    private boolean take(String x){if(s.startsWith(x,p)){p+=x.length();return true;}return false;}private boolean peek(char c){return p<s.length()&&s.charAt(p)==c;}private void expect(char c){ws();if(!peek(c))throw err("expected "+c);p++;}private RuntimeException err(String x){return new IllegalArgumentException("JSON at "+p+": "+x);}
}
