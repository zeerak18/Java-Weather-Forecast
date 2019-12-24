import java.util.Date;

/* helper class for dealing with the api data. Forecasts from 
the OpenWeatherMap come in intervals, like from "2019-01-01 09-00"
to "2019-01-01 21-00"
*/

public class TimeInterval {
    private int position;
    private Date from;
    private Date to;

    public TimeInterval(int position, Date from, Date to) {
        this.position = position;
        this.from = from;
        this.to = to;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return String.format("TimeInterval{from=%s, to=%s}", from, to);
    }
}
