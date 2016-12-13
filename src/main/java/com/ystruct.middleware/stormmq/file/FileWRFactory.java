package file;


/**
 * Created by yang on 16-12-1.
 */
public class FileWRFactory {
    public FileWRFactory(){

    }
    FileHandler GetDefaultFileHandler(String filePath) {
	//	return new DefaulteFileHandler();
        assert(false);
        return null;
    }
    FileHandler getRandomAccessFileHandler() {
        return new RandomAccessFileHandler();
    }
    FileHandler getChannelFileHandler(){
        return new ChannelFileHander();
    }
}
