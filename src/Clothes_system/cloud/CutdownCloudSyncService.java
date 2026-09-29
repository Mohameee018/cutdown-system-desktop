package Clothes_system.cloud;

import Clothes_system.Product;
import Clothes_system.db.PersistenceRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Serializes a desktop product snapshot and sends it to the protected
 * Cutdown API. No Supabase credentials are stored in the desktop app.
 */
public final class CutdownCloudSyncService {
    private CutdownCloudSyncService() {}

    public static void syncProductAsync(Product product) {
        if (product == null || !CutdownCloudConfig.configured()) return;
        Thread t = new Thread(() -> {
            try {
                new CutdownCloudClient().postJson(
                    "/api/desktop/products/sync",
                    productJson(product)
                );
            } catch (Exception ex) {
                System.err.println("Cutdown cloud product sync failed: " + ex.getMessage());
            }
        }, "cutdown-product-sync");
        t.setDaemon(true);
        t.start();
    }

    private static String productJson(Product p) throws IOException {
        StringBuilder b = new StringBuilder(8192);
        b.append("{\"product\":{");
        field(b,"desktop_id",p.getId()).append(',');
        field(b,"sku",p.getSku()).append(',');
        field(b,"name",p.getName()).append(',');
        field(b,"category",p.getCategory()).append(',');
        field(b,"description",p.getDescription()).append(',');
        field(b,"image_path",p.getImagePath()).append(',');
        number(b,"price",p.getSellingPrice()).append(',');
        number(b,"cost_price",p.getCostPrice()).append(',');
        number(b,"stock",p.getStockQuantity()).append(',');
        number(b,"minimum_stock",p.getMinimumStock()).append(',');
        b.append("\"active\":").append(p.isActive()).append(',');
        b.append("\"variants\":[");
        for (int i=0;i<p.getVariants().size();i++) {
            Product.ProductVariant v=p.getVariants().get(i);
            if(i>0)b.append(',');
            b.append('{');
            field(b,"desktop_variant_id",v.getId()).append(',');
            field(b,"size",v.getSize()).append(',');
            field(b,"color",v.getColor()).append(',');
            number(b,"stock",v.getStockQuantity()).append(',');
            b.append("\"active\":true");
            b.append('}');
        }
        b.append("],\"images\":[");
        Map<String,List<String>> images=PersistenceRepository.readProductColorImages(p.getId());
        int n=0;
        for(Map.Entry<String,List<String>> e:images.entrySet()){
            List<String> paths=e.getValue();
            for(int i=0;i<paths.size() && i<3;i++){
                Path path=Path.of(paths.get(i));
                if(!Files.isRegularFile(path)) continue;
                long size=Files.size(path);
                if(size>1_500_000) continue;
                String mime=Files.probeContentType(path);
                if(mime==null) mime=mimeFor(path.toString());
                if(n++>0)b.append(',');
                b.append('{');
                field(b,"color",e.getKey()).append(',');
                number(b,"sort_order",i).append(',');
                field(b,"mime_type",mime).append(',');
                field(b,"alt_text",p.getName()).append(',');
                field(b,"data_base64",Base64.getEncoder().encodeToString(Files.readAllBytes(path)));
                b.append('}');
            }
        }
        // The main image is selected in Add Product before color-photo fields exist.
        // Always sync it as the default product image so a newly-created product is visible on the website.
        if(n==0 && p.getImagePath()!=null && !p.getImagePath().isBlank()){
            Path path=Path.of(p.getImagePath());
            if(Files.isRegularFile(path)){
                long size=Files.size(path);
                if(size<=1_500_000){
                    String mime=Files.probeContentType(path);
                    if(mime==null) mime=mimeFor(path.toString());
                    b.append('{');
                    field(b,"color","").append(',');
                    number(b,"sort_order",0).append(',');
                    field(b,"mime_type",mime).append(',');
                    field(b,"alt_text",p.getName()).append(',');
                    field(b,"data_base64",Base64.getEncoder().encodeToString(Files.readAllBytes(path)));
                    b.append('}');
                }
            }
        }
        b.append("]}}");
        return b.toString();
    }

    private static String mimeFor(String path){
        String s=path.toLowerCase();
        if(s.endsWith(".png")) return "image/png";
        if(s.endsWith(".webp")) return "image/webp";
        if(s.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private static StringBuilder field(StringBuilder b,String k,String v){
        b.append('\"').append(escape(k)).append("\":\"").append(escape(v==null?"":v)).append('\"');
        return b;
    }
    private static StringBuilder number(StringBuilder b,String k,double v){
        b.append('\"').append(k).append("\":").append(v);
        return b;
    }
    private static String escape(String s){
        return s.replace("\\\\","\\\\\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");
    }
}
