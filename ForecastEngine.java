import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * An entry point for Forecast Server
 * and an implementation of ForecastService contract
 * at the same time.
 */
public class ForecastEngine implements ForecastService {

    private final Map<Integer, OpenWeatherMap> storage = new HashMap<>();

    public static void main(String[] args) throws RemoteException {

     /*   if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        ForecastService forecastEngine = new ForecastEngine();
        ForecastService remote = (ForecastService) UnicastRemoteObject.exportObject(forecastEngine, 0);
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind("ForecastService", remote);

        System.out.println("INFO: Registered in rmiregistry");
    } */
    
    ForecastService forecastEngine = new ForecastEngine();
    ForecastService remote = (ForecastService) UnicastRemoteObject.exportObject(forecastEngine, 0);
    Registry registry = LocateRegistry.getRegistry();
    registry.rebind("ForecastService", remote);

    System.out.println("INFO: Registered in rmiregistry");
    
    }

    @Override
    public List<Date> queryForecast(Integer cityId) throws RemoteException {
        return findWeatherMap(cityId).dates();
    }

    private OpenWeatherMap findWeatherMap(Integer cityId) {
        if (storage.containsKey(cityId)) {
            System.out.println("INFO: Return already fetched data for " + cityId);
            return storage.get(cityId);
        }

        try {
            OpenWeatherMap weatherMap = new OpenWeatherMap(cityId);
            storage.put(cityId, weatherMap);
            return weatherMap;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage(), ioe);
        }
    }

    @Override
    public Forecast makeForecast(Integer cityId, Date date) throws RemoteException {
        return findWeatherMap(cityId).forecast(date);
    }

    @Override
    public void invalidate(Integer cityId) throws RemoteException {
        storage.remove(cityId);
    }
}
