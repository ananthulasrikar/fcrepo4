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

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Stream.empty;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.utils.Streams.fromIterator;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/9/14
 */
public class HashRdfContext extends NodeRdfContext {


    private static final List<Class<? extends NodeRdfContext>> TRIPLE_GENERATORS = of(TypeRdfContext.class, PropertiesRdfContext.class, BlankNodeRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     */
    public HashRdfContext(final FedoraResource resource,
            final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        super(resource, idTranslator);
    }

    @Override
    public Stream<Triple> applyThrows(final Node node) throws RepositoryException {
        if (node.hasNode("#")) {
            final Iterator<Node> hashChildrenNodesIterator = node.getNode("#").getNodes();
            final Stream<Node> hashChildren = fromIterator(hashChildrenNodesIterator);
            return hashChildren.flatMap(n -> nodeConverter.convert(n).getTriples(translator(), TRIPLE_GENERATORS));
        }
        return empty();
    }
}