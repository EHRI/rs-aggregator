package nl.knaw.dans.rs.aggregator.xml;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class RsItem<T extends RsItem> {

  private String loc;
  private ZonedDateTime lastmod;
  private String changefreq;
  @XmlElement(name = "md", namespace = "http://www.openarchives.org/rs/terms/")
  private RsMd rsMd;
  @XmlElement(name = "ln", namespace = "http://www.openarchives.org/rs/terms/")
  private List<RsLn> rsLnList = new ArrayList<>();

  public String getLoc() {
    return loc;
  }

  @SuppressWarnings("unchecked")
  public T withLoc(@Nonnull String loc) {
    this.loc = Objects.requireNonNull(loc);
    return (T) this;
  }

  public Optional<ZonedDateTime> getLastmod() {
    return Optional.ofNullable(lastmod);
  }

  @SuppressWarnings("unchecked")
  public T withLastmod(ZonedDateTime lastmod) {
    this.lastmod = lastmod;
    return (T) this;
  }

  public Optional<String> getChangefreq() {
    return Optional.ofNullable(changefreq);
  }

  @SuppressWarnings("unchecked")
  public T withChangefreq(String changefreq) {
    this.changefreq = changefreq;
    return (T) this;
  }

  public Optional<RsMd> getMetadata() {
    return Optional.ofNullable(rsMd);
  }

  @SuppressWarnings("unchecked")
  public T withMetadata(RsMd rsMd) {
    this.rsMd = rsMd;
    return (T) this;
  }

  public List<RsLn> getLinkList() {
    return rsLnList;
  }

  @SuppressWarnings("unchecked")
  public T addLink(RsLn rsLn) {
    rsLnList.add(rsLn);
    return (T) this;
  }

  public ZonedDateTime getRsMdDateTime() {
    return getMetadata()
      .flatMap(RsMd::getDateTime)
      .orElse(null);
  }

  public ZonedDateTime getRsMdAt() {
    return getMetadata()
      .flatMap(RsMd::getAt)
      .orElse(null);
  }

  public ZonedDateTime getRsMdFrom() {
    return getMetadata()
      .flatMap(RsMd::getFrom)
      .orElse(null);
  }

  public int isZDTAfter(RsItem ot) {
    ZonedDateTime myZDT = getRsMdDateTime();
    ZonedDateTime otZDT = ot.getRsMdDateTime();
    myZDT = myZDT == null ? getRsMdAt() : myZDT;
    otZDT = otZDT == null ? ot.getRsMdAt() : otZDT;
    myZDT = myZDT == null ? getRsMdFrom() : myZDT;
    otZDT = otZDT == null ? ot.getRsMdFrom() : otZDT;

    if (myZDT == null || otZDT == null) return 0;
    if (myZDT.equals(otZDT)) return 0;
    return myZDT.isAfter(otZDT) ? 1 : -1;
  }

  public int isLastModAfter(RsItem ot) {
    if (lastmod == null || ot.lastmod == null) return 0;
    if (lastmod.equals(ot.lastmod)) return 0;
    return lastmod.isAfter(ot.lastmod) ? 1 : -1;
  }

  public int isAfter(RsItem ot) {
    int dateTimeAfter = isZDTAfter(ot);
    if (dateTimeAfter != 0) return dateTimeAfter;
    return isLastModAfter(ot);
  }

  @SuppressWarnings("unchecked")
  public T latest(T other) {
    if (other == null) return (T) this;
    return isAfter(other) > 0 ? (T) this : other;
  }

}
