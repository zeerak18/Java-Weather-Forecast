import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static javax.xml.xpath.XPathConstants.STRING;

// gateway to weather api


final class OpenWeatherMap {
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final String key = "2694490f80a0d428d0b8c669a70d75d6";
    private static final String base = "http://api.openweathermap.org/data/2.5/forecast?id=%d&APPID=%s&mode=xml&units=metric";
    private final String content;

    OpenWeatherMap(Integer location) throws IOException {
        String url = String.format(base, location, key);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.setConnectTimeout(DEFAULT_TIMEOUT);
        connection.setReadTimeout(DEFAULT_TIMEOUT);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            this.content = content.toString();
        } finally {
            connection.disconnect();
        }
    }

    public Forecast forecast(Date date) {
        return new ForecastReader(content).forecast(date);
    }

    public List<Date> dates() {
        return new ForecastReader(content).getDates();
    }

    static final class ForecastReader {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

        private final Document content;
        private final XPathFactory pathFactory = XPathFactory.newInstance();

        ForecastReader(String content) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(false);
                factory.setNamespaceAware(true);
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(true);
                DocumentBuilder builder = factory.newDocumentBuilder();

                this.content = builder.parse(new InputSource(new StringReader(content)));
            } catch (ParserConfigurationException | IOException | SAXException e) {
                throw new RuntimeException(e);
            }
        }

        private String $(String expr) {
            XPath xpath = pathFactory.newXPath();
            try {
                return (String) xpath.evaluate(expr, content, STRING);
            } catch (XPathExpressionException e) {
                return "$-1000";
            }
        }

        public Collection<TimeInterval> getTimeIntervals() {
            Collection<TimeInterval> intervals = new ArrayList<>();

            XPath xpath = pathFactory.newXPath();
            try {
                NodeList nl = (NodeList) xpath.evaluate("/weatherdata/forecast/time", this.content, XPathConstants.NODESET);

                for (int i = 1; i < nl.getLength(); i++) {
                    String from = $("/weatherdata/forecast/time[" + i + "]/attribute::from");
                    String to = $("/weatherdata/forecast/time[" + i + "]/attribute::to");

                    intervals.add(new TimeInterval(i, dateFormat.parse(from), dateFormat.parse(to)));
                }
            } catch (XPathExpressionException | ParseException e) {
                throw new RuntimeException(e);
            }

            return intervals;
        }

        public List<Date> getDates() {
            Set<Date> dates = new TreeSet<>();

            XPath xpath = pathFactory.newXPath();
            try {
                NodeList nl = (NodeList) xpath.evaluate("/weatherdata/forecast/time", this.content, XPathConstants.NODESET);

                for (int i = 1; i < nl.getLength(); i++) {
                    String from = $("/weatherdata/forecast/time[" + i + "]/attribute::from");

                    dates.add(Utils.truncate(dateFormat.parse(from)));
                }
            } catch (XPathExpressionException | ParseException e) {
                throw new RuntimeException(e);
            }

            return new ArrayList<>(dates);
        }

        public Forecast forecast(Date date) {
            for (TimeInterval interval : getTimeIntervals()) {
                Date from = Utils.truncate(interval.getFrom());
                if (from.equals(date)) {
                    int position = interval.getPosition() + 1;
                    String base = "/weatherdata/forecast/time[" + position + "]";
                    String location = $("/weatherdata//location/name");
                    String temp = $(base + "/temperature/attribute::value") + "C";
                    String windSpeed = $(base + "/windSpeed/attribute::mps") + "m/s";
                    String pressure = $(base + "/pressure/attribute::value") + "hPa";
                    String humidity = $(base + "/humidity/attribute::value") + "%";
                    String clouds = $(base + "/clouds/attribute::value");

                    return new Forecast(location,
                            temp,
                            windSpeed,
                            pressure,
                            humidity,
                            clouds
                    );
                }
            }

            return null;
        }
    }

    static final class Utils {
        static Date truncate(Date date) {
            Calendar cal = Calendar.getInstance(); // locale-specific
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long time = cal.getTimeInMillis();
            return new Date(time);
        }
    }
}
