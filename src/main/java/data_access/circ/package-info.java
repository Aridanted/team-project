/**
 * Reads Logisim-evolution {@code .circ} files into {@link entity.circuit.Circuit}s.
 *
 * <p><b>You do not need to read this package to add a feature.</b> The only public entry point
 * is {@link data_access.circ.LogisimCircuitParser#load(String)}; everything else is
 * package-private. Order of work inside: {@code CircXmlReader} (XML), {@code ComponentRegistry}
 * (what each component is and where its ports are), {@code NetBuilder} (which points are
 * connected), {@code PinLabeler} (usable pin names), {@code ConnectivityChecker} (wiring errors
 * and warnings). Error and warning texts are all in {@code Diagnostics}.
 */
package data_access.circ;
