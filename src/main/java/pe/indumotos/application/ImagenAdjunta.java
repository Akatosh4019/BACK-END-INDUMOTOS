package pe.indumotos.application;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public final class ImagenAdjunta {
 private ImagenAdjunta(){}
 public static String validar(byte[] bytes){
  if(bytes==null||bytes.length==0||bytes.length>5*1024*1024)throw error();
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw error();
   var reader=readers.next();try{
    reader.setInput(input);String format=reader.getFormatName().toLowerCase();
    if(!format.equals("png")&&!format.equals("jpeg")&&!format.equals("jpg"))throw error();
    int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>4096||h>4096)throw error();
    if(reader.read(0)==null)throw error();return format.equals("png")?"image/png":"image/jpeg";
   }finally{reader.dispose();}
  }catch(org.springframework.web.server.ResponseStatusException ex){throw ex;}catch(Exception ex){throw error();}
 }
 private static ResponseStatusException error(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona una imagen JPG/PNG válida de hasta5MB y4096px");}
}
