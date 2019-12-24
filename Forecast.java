import java.io.Serializable;

//Just a model to transfer info between the server and client

public class Forecast implements Serializable {
    private final String location;
    private final String temperature;
    private final String windSpeed;
    private final String pressure;
    private final String humidity;
    private final String clouds;

    Forecast(String location, String temperature, String windSpeed, String pressure, String humidity, String clouds) {
        this.location = location;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.pressure = pressure;
        this.humidity = humidity;
        this.clouds = clouds;
    }

    public String getLocation() {
        return location;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public String getPressure() {
        return pressure;
    }

    public String getHumidity() {
        return humidity;
    }

    public String getClouds() {
        return clouds;
    }

    @Override
    public String toString() {
        return String.format("Forecast{temperature='%s', windSpeed='%s', pressure='%s', humidity='%s', clouds='%s'}",
                temperature, windSpeed, pressure, humidity, clouds);
    }
}
