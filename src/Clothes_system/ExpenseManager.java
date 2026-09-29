package Clothes_system;

import Clothes_system.db.PersistenceRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ExpenseManager {
    public static class Expense {
        private final String id;
        private Date date;
        private String category;
        private String description;
        private String payment;
        private double amount;
        private String status;

        Expense(String id, Date date, String category, String description,
                String payment, double amount, String status) {
            this.id=id; this.date=date; this.category=category;
            this.description=description; this.payment=payment;
            this.amount=amount; this.status=status;
        }
        public String getId(){return id;}
        public Date getDate(){return date;}
        public void setDate(Date d){date=d;}
        public String getCategory(){return category;}
        public void setCategory(String v){category=v;}
        public String getDescription(){return description;}
        public void setDescription(String v){description=v;}
        public String getPayment(){return payment;}
        public void setPayment(String v){payment=v;}
        public double getAmount(){return amount;}
        public void setAmount(double v){amount=v;}
        public String getStatus(){return status;}
        public void setStatus(String v){status=v;}
    }

    private static final List<Expense> expenses = new ArrayList<>();
    private static int counter = 1;

    private ExpenseManager(){}

    public static synchronized List<Expense> getExpenses(){ return new ArrayList<>(expenses); }
    public static synchronized double total(){ double x=0; for(Expense e:expenses)x+=e.amount; return x; }
    public static synchronized double totalBetween(Date from, Date to){
        double x=0; for(Expense e:expenses) if(!e.date.before(from)&&!e.date.after(to)) x+=e.amount; return x;
    }
    public static synchronized int countBetween(Date from, Date to){
        int x=0; for(Expense e:expenses) if(!e.date.before(from)&&!e.date.after(to)) x++; return x;
    }
    public static synchronized Expense add(Date date,String category,String description,String payment,double amount,String status){
        if(date==null||category==null||category.trim().isEmpty()||amount<=0) throw new IllegalArgumentException("Invalid expense data.");
        Expense e=new Expense(String.format(Locale.US,"EXP-%04d",counter++),date,category.trim(),description==null?"":description.trim(),payment,amount,status==null||status.isEmpty()?"Paid":status);
        // constructor argument order correction
        e.amount=amount;
        expenses.add(e);
        PersistenceRepository.saveExpenses();
        return e;
    }
    public static synchronized void update(Expense e,Date date,String category,String description,String payment,double amount,String status){
        if(e==null||date==null||category==null||category.trim().isEmpty()||amount<=0) throw new IllegalArgumentException("Invalid expense data.");
        e.date=date;e.category=category.trim();e.description=description==null?"":description.trim();e.payment=payment;e.amount=amount;e.status=status;
        PersistenceRepository.saveExpenses();
    }
    public static synchronized void delete(Expense e){if(e!=null && expenses.remove(e)) PersistenceRepository.saveExpenses();}
    public static synchronized void clearPersistenceData(){ expenses.clear(); counter=1; }
    public static synchronized void addPersisted(String id, Date date, String category, String description, String payment, double amount, String status){
        if(id==null || date==null) return;
        expenses.add(new Expense(id,date,category==null?"":category,description==null?"":description,payment,amount,status==null?"Paid":status));
        try{if(id.startsWith("EXP-")) counter=Math.max(counter,Integer.parseInt(id.substring(4))+1);}catch(Exception ignored){}
    }

    public static String formatDate(Date d){return new SimpleDateFormat("dd MMM yyyy",Locale.ENGLISH).format(d);}
}
