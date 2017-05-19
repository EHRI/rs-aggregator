package nl.knaw.dans.rs.aggregator.xml;

/**
 * A mandatory attribute in all ResourceSync documents.
 * <p>Defined values are <code>resourcelist, changelist, resourcedump, changedump, resourcedump-manifest,
 * changedump-manifest, capabilitylist, and description</code>.</p>
 *
 * @see <a href="http://www.openarchives.org/rs/1.1/resourcesync#DocumentFormats">
 *   http://www.openarchives.org/rs/1.1/resourcesync#DocumentFormats</a>
 */
public enum Capability {

  /**
   * A Source Description is a mandatory document that enumerates the Capability Lists offered by a Source.
   */
  DESCRIPTION("description", 3),

  /**
   * A Capability List is a document that enumerates all capabilities supported by a Source for a specific set of resources.
   */
  CAPABILITYLIST("capabilitylist", 2),

  /**
   * A Resource List is introduced to list and describe the resources that a Source makes available for synchronization.
   */
  RESOURCELIST("resourcelist", 1),

  /**
   * A Source may publish a Resource Dump, which provides links to packages of the resources' bitstreams.
   */
  RESOURCEDUMP("resourcedump", 1),

  /**
   * Each ZIP package referred to from a Resource Dump must contain a Resource Dump Manifest file
   * that describes the package's constituent bitstreams.
   */
  RESOURCEDUMP_MANIFEST("resourcedump-manifest", 0),

  /**
   * A Change List is a document that contains a description of changes to a Source's resources.
   */
  CHANGELIST("changelist", 1),

  /**
   * A Change Dump is a document that points to packages containing bitstreams for the Source's changed resources.
   */
  CHANGEDUMP("changedump", 1),

  /**
   * Each ZIP package referred to from a Change Dump must contain a Change Dump Manifest file that
   * describes the constituent bitstreams of the package.
   */
  CHANGEDUMP_MANIFEST("changedump-manifest", 0);

  /**
   * Get the Capability corresponding to the given <code>xmlValue</code>.
   *
   * @param xmlValue String as present in xml
   * @return the corresponding Capability
   * @throws IllegalArgumentException if the given string does not correspond to any Capability
   */
  public static Capability forString(String xmlValue) throws IllegalArgumentException {
    String name = xmlValue.toUpperCase().replace('-', '_');
    return valueOf(name);
  }

  /**
   * Get the level of the Capability corresponding to the given <code>xmlValue</code>.
   * The hierarchy of sitemap documents has 4 levels, with <code>description</code> as the highest level (3)
   * and <code>resourcedump-manifest</code> and <code>changedump-manifest</code> at the lowest level (0).
   *
   * @param xmlValue String as present in xml
   * @return the level of the Capability or -1 if the given string does not correspond to any Capability
   */
  public static int levelfor(String xmlValue) {
    String name = xmlValue.toUpperCase().replace('-', '_');
    try {
      return valueOf(name).level;
    } catch (IllegalArgumentException e) {
      return -1;
    }
  }

  /**
   * The value as present in xml on the element <code>rs:md</code> on the attribute <code>capability</code>.
   */
  public final String xmlValue;

  /**
   * The level of the Capability.
   * The hierarchy of sitemap documents has 4 levels, with <code>description</code> as the highest level (3)
   * and the <code>resourcedump-manifest</code> and <code>changedump-manifest</code>  at the lowest level (0).
   */
  public final int level;


  Capability(String xmlValue, int level) {
    this.xmlValue = xmlValue;
    this.level = level;
  }

  /**
   * Get the {@link Capability#xmlValue} of this Capability.
   *
   * @return the xmlValue of this Capability
   */
  public String getXmlValue() {
    return xmlValue;
  }

  /**
   * Get the {@link Capability#level} of this Capability.
   *
   * @return the level of this Capability
   */
  public int getLevel() {
    return level;
  }

  /**
   * The capability of a parent document expressed with a link with relation type 'up'. Except for documents with
   * capability 'description' such a link is mandatory.
   *
   * @return the capability of a parent document with relation type 'up', or <code>null</code> if such a
   *     relation does not exist.
   */
  public Capability getUpRelation() {
    if (this == DESCRIPTION) {
      return null;
    } else if (this == CAPABILITYLIST) {
      return DESCRIPTION;
    } else {
      return CAPABILITYLIST;
    }
  }

  /**
   * The capability of a parent document expressed with a link with relation type 'index'.
   *
   * @return the capability of a parent document with relation type 'index', or <code>null</code> if such a
   *     relation does not exist.
   */
  public Capability getIndexRelation() {
    if (this == RESOURCEDUMP_MANIFEST || this == CHANGEDUMP_MANIFEST) {
      return null;
    } else {
      return this;
    }
  }

  /**
   * The capability of possible child documents as expressed in the &lt;loc&gt; element of &lt;url&gt; and
   * &lt;sitemap&gt;.
   *
   * @return Array of Capabilities of possible child documents.
   */
  public Capability[] getChildRelations() {
    // child relations as expressed in de <loc> element of <url> and <sitemap>.
    // 'this' only allowed if relation from index; index can have children with same capability.
    // manifest files are related to by means of <rs:ln> element with type 'contents'.
    if (this == DESCRIPTION) {
      return new Capability[]{this, CAPABILITYLIST};
    } else if (this == CAPABILITYLIST) {
      return new Capability[]{this, RESOURCELIST, RESOURCEDUMP, CHANGELIST, CHANGEDUMP};
    } else if (this == RESOURCELIST || this == RESOURCEDUMP || this == CHANGELIST || this == CHANGEDUMP) {
      return new Capability[]{this};
    } else {
      return new Capability[]{};
    }
  }

  /**
   * The xmlValues of possible child documents as expressed in the &lt;loc&gt; element of &lt;url&gt; and
   * &lt;sitemap&gt;.
   *
   * @return Array of xmlValues of possible child documents
   *
   * @see Capability#getChildRelations()
   */
  public String[] getChildRelationsXmlValues() {
    Capability[] childRelations = getChildRelations();
    String[] xmlValues = new String[childRelations.length];
    for (int i = 0; i < childRelations.length; i++) {
      xmlValues[i] = childRelations[i].xmlValue;
    }
    return xmlValues;
  }

  /**
   * Verify that the given Capability is a valid value for the <code>up</code> relation in a document
   * of this Capability.
   *
   * @param relation the Capability to test
   * @return <code>true</code> if the <code>up</code> relation is valid, <code>false</code> otherwise.
   * @see Capability#getUpRelation()
   */
  public boolean verifyUpRelation(Capability relation) {
    return this.getUpRelation() == relation;
  }

  /**
   * Verify that the given Capability is a valid value for the <code>index</code> relation in a document
   * of this Capability.
   *
   * @param relation the Capability to test
   * @return <code>true</code> if the <code>index</code> relation is valid, <code>false</code> otherwise.
   * @see Capability#getIndexRelation()
   */
  public boolean verifyIndexRelation(Capability relation) {
    return  this.getIndexRelation() == relation;
  }

  /**
   * Verify that the given Capability is a valid value for possible child documents as expressed in
   * the &lt;loc&gt; element of &lt;url&gt; and &lt;sitemap&gt;.
   *
   * @param relation the Capability to test
   * @return <code>true</code> if the child relation is valid, <code>false</code> otherwise.
   * @see Capability#getChildRelations()
   */
  public boolean verifyChildRelation(Capability relation) {
    boolean allowed = false;
    Capability[] childRelations = getChildRelations();
    if (relation == null && childRelations.length == 0) {
      allowed = true;
    } else {
      for (Capability capa : childRelations) {
        if (capa == relation) {
          allowed = true;
          break;
        }
      }
    }
    return allowed;
  }

}
