import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

/**
 * A contract between client and server
 */
public interface ForecastService extends Remote {

    List<Date> queryForecast(Integer cityId) throws RemoteException;

    Forecast makeForecast(Integer cityId, Date date) throws RemoteException;

    void invalidate(Integer cityId) throws RemoteException;
}
