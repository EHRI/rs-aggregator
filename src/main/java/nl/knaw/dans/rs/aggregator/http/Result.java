package nl.knaw.dans.rs.aggregator.http;


import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Result<T> implements Consumer<T>, Comparable<Result> {

  private URI uri;
  private int ordinal;
  private String statusLine;
  private int statusCode;
  private T content;
  private List<Throwable> errors = new ArrayList<>();
  private Map<String, String> headers = new HashMap<>();
  private Map<URI, Result<?>> parents = new HashMap<>();
  private Map<URI, Result<?>> children = new HashMap<>();
  private Set<String> invalidUris = new TreeSet<>();

  public Result(URI uri) {
    this.uri = uri == null ? null : UriRegulator.regulate(uri).orElse(null);
  }

  public URI getUri() {
    return uri;
  }

  public String getStatusLine() {
    return statusLine;
  }

  public void setStatusLine(String statusLine) {
    this.statusLine = statusLine;
  }

  public int getOrdinal() {
    return ordinal;
  }

  public void setOrdinal(int ordinal) {
    this.ordinal = ordinal;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public Optional<T> getContent() {
    return Optional.ofNullable(content);
  }

  public List<Throwable> getErrors() {
    return Collections.unmodifiableList(errors);
  }

  public void addError(Throwable error) {
    errors.add(error);
  }

  public Throwable lastError() {
    if (errors.isEmpty()) {
      return null;
    } else {
      return errors.get(errors.size() - 1);
    }
  }

  public Set<String> getInvalidUris() {
    return invalidUris;
  }

  public void addInvalidUri(String invalidUri) {
    invalidUris.add(invalidUri);
  }

  @Override
  public void accept(T content) {
    this.content = content;
  }

  public Map<URI, Result<?>> getParents() {
    return parents;
  }

  public Map<URI, Result<?>> getChildren() {
    return children;
  }

  public <R> Result<R> map(Function<T, R> func) {
    Result<R> copy = new Result<R>(uri);

    copy.statusCode = statusCode;
    copy.ordinal = ordinal;
    copy.errors.addAll(errors);
    copy.invalidUris.addAll(invalidUris);
    copy.parents.putAll(parents);
    copy.children.putAll(children);

    if (content != null) {
      copy.accept(func.apply(content));
    }

    return copy;
  }

  public void addParent(Result<?> parent) {
    if (!parents.containsKey(parent.getUri())) {
      parents.put(parent.getUri(), parent);
      parent.addChild(this);
    }
  }

  public void addChild(Result<?> child) {
    if (!children.containsKey(child.getUri())) {
      children.put(child.getUri(), child);
      child.addParent(this);
    }
  }

  @Override
  public String toString() {
    return new StringBuilder(super.toString())
      .append(", uri=").append(uri.toString())
      .append(", statusLine=").append(getStatusLine())
      .append(", content=").append(getContent().isPresent() ? getContent().get() : "<empty>")
      .append(", errors=").append(getErrors().size() > 0 ? getErrors().stream()
        .map(throwable -> throwable.toString().replaceAll("\n", " * "))
        .collect(Collectors.joining("; ")) : "<empty>")
      .append(", parents=").append(getParents().size() > 0 ? getParents().keySet().stream()
        .map(URI::toString).collect(Collectors.joining("; ")) : "<empty>")
      .append(", children=").append(getChildren().size() > 0 ? getChildren().keySet().stream()
        .map(URI::toString).collect(Collectors.joining("; ")) : "<empty>")
      .append(", invalidUris=").append(getInvalidUris().size() > 0 ? getInvalidUris().stream()
        .collect(Collectors.joining("; ")) : "<empty>")
      .toString();

  }

  @Override
  public int compareTo(@Nullable Result o) {
    if (o == null) {
      return -1;
    } else if (uri == null && o.uri == null) {
      return 0;
    } else if (o.uri == null) {
      return -1;
    } else if (uri == null) {
      return 1;
    } else {
      return uri.compareTo(o.uri);
    }
  }
}
