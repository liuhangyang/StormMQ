package serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


/**
 * Created by yang on 16-11-22.
 */
public class KryoSerialization {
    private Kryo kryo;
    private Registration registration = null;
    private Class<?> t;
    public KryoSerialization(){
        kryo = new Kryo();
        kryo.setReferences(true);
    }
    public void register(Class<?> T){
        //注册类
        t = T;
        registration = kryo.register(t);
    }
    public byte[] Serialize(Object object){
        Output output = null;
        output = new Output(1,4096);
        kryo.writeObject(output,object);
        byte[] bt = output.toBytes();
        output.flush();
        return bt;
    }
    public <t> t Deserialize(byte[] bt){
        Input input = null;
        input = new Input(bt);
        t res = (t)kryo.readObject(input,registration.getType());
        input.close();
        return res;
    }
}
