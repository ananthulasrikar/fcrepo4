
package org.fcrepo.jms.headers;

import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jms.JMSException;
import javax.jms.Message;

import org.fcrepo.jms.observer.JMSEventMessageFactory;
import org.fcrepo.kernel.utils.EventType;

/**
 * Generates JMS {@link Message}s composed entirely of headers, based entirely
 * on information found in the JCR {@link Event} that triggers publication.
 *
 * @author ajs6f
 * @date Dec 2, 2013
 */
public class DefaultMessageFactory implements JMSEventMessageFactory {

    public static final String TIMESTAMP_HEADER_NAME = REPOSITORY_NAMESPACE
            + "timestamp";

    public static final String IDENTIFIER_HEADER_NAME = REPOSITORY_NAMESPACE
            + "identifier";

    public static final String EVENT_TYPE_HEADER_NAME = REPOSITORY_NAMESPACE
            + "eventType";

    @Override
    public Message getMessage(final Event jcrEvent, final Session jcrSession,
            final javax.jms.Session jmsSession) throws RepositoryException,
                                               IOException, JMSException {
        final Message message = jmsSession.createMessage();
        message.setLongProperty(TIMESTAMP_HEADER_NAME, jcrEvent.getDate());
        message.setStringProperty(IDENTIFIER_HEADER_NAME, jcrEvent.getPath());
        message.setStringProperty(EVENT_TYPE_HEADER_NAME, getEventURI(jcrEvent
                .getType()));
        return message;
    }

    private static String getEventURI(final int type) {
        return REPOSITORY_NAMESPACE + EventType.valueOf(type).toString();
    }

}
