/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.rdf.impl;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.utils.UncheckedFunction;
import org.fcrepo.kernel.utils.UncheckedPredicate;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyValueStream;

import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.stream.Stream.empty;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.utils.Streams.fromIterator;
import static org.fcrepo.kernel.utils.UncheckedFunction.uncheck;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/25/14
 */
public class LdpContainerRdfContext extends NodeRdfContext {
    private static final Logger LOGGER = getLogger(ChildrenRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     * @throws javax.jcr.RepositoryException
     */
    public LdpContainerRdfContext(final FedoraResource resource,
                                  final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);
        final Iterator<Property> memberReferences = resource.getNode().getReferences(LDP_MEMBER_RESOURCE);
        final Stream<Property> properties = fromIterator(memberReferences).filter(isContainer);
        concat(properties.flatMap(property2triples));
    }

    private static final Predicate<Property> isContainer = UncheckedPredicate.uncheck(property -> {
            final Node container = property.getParent();
            return container.isNodeType(LDP_DIRECT_CONTAINER) || container.isNodeType(LDP_INDIRECT_CONTAINER);
    });

    private final Function<Property, Stream<Triple>> property2triples = uncheck(p -> memberRelations(nodeConverter
            .convert(p.getParent())));

    /**
     * Get the member relations assert on the subject by the given node
     * @param container
     * @return
     * @throws RepositoryException
     */
    private Stream<Triple> memberRelations(final FedoraResource container) throws RepositoryException {
        final com.hp.hpl.jena.graph.Node memberRelation;

        if (container.hasProperty(LDP_HAS_MEMBER_RELATION)) {
            final Property property = container.getProperty(LDP_HAS_MEMBER_RELATION);
            memberRelation = createURI(property.getString());
        } else if (container.hasType(LDP_BASIC_CONTAINER)) {
            memberRelation = LDP_MEMBER.asNode();
        } else {
            return empty();
        }

        final String insertedContainerProperty;

        if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            if (container.hasProperty(LDP_INSERTED_CONTENT_RELATION)) {
                insertedContainerProperty = container.getProperty(LDP_INSERTED_CONTENT_RELATION).getString();
            } else {
                return empty();
            }
        } else {
            insertedContainerProperty = MEMBER_SUBJECT.getURI();
        }

        final Stream<FedoraResource> memberNodes = container.getChildren();
        return memberNodes.flatMap(UncheckedFunction.uncheck(child -> {
            final com.hp.hpl.jena.graph.Node childSubject;
            if (child instanceof NonRdfSourceDescription) {
                childSubject = translator().reverse()
                        .convert(((NonRdfSourceDescription) child).getDescribedResource())
                        .asNode();
            } else {
                childSubject = translator().reverse().convert(child).asNode();
            }

            if (insertedContainerProperty.equals(MEMBER_SUBJECT.getURI())) {
                return Stream.of(create(subject(), memberRelation, childSubject));
            }
            final String insertedContentProperty = getPropertyNameFromPredicate(resource().getNode(),
                    createResource(insertedContainerProperty), null);

            if (!child.hasProperty(insertedContentProperty)) {
                return empty();
            }

            final Stream<Value> values = new PropertyValueStream(child.getProperty(insertedContentProperty));
            return values.map(v -> create(subject(), memberRelation, new ValueConverter(session(),
                    translator()).convert(v).asNode()));
        }));
    }
}
