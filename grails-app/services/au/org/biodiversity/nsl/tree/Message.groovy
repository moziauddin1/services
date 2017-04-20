/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL tree services plugin project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package au.org.biodiversity.nsl.tree

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.ArrangementType
import au.org.biodiversity.nsl.Instance
import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.Node
import au.org.biodiversity.nsl.Reference
import grails.util.Holders
import org.springframework.context.MessageSourceResolvable
import org.springframework.context.i18n.LocaleContextHolder

class Message implements MessageSourceResolvable {
    public final Msg msg
    public final List args = []
    public final List nested = []

    Message(Msg msg, args = null) {
        this.msg = msg
        if (!msg) throw new IllegalArgumentException('null msg')
        if (args) {
            if (args instanceof Collection) {
                this.args.addAll((Collection) args)
            } else {
                this.args.add args
            }

        }
    }

    static Message makeMsg(Msg msg, args = null) {
        return new Message(msg, args)
    }

    // this is for loggers, etc. The web pages should use a template to render these objects.

    String toString() {
        return getLocalisedString()
    }

    String getLocalisedString() {
        StringBuilder sb = new StringBuilder()
        buildNestedString(sb, 0)
        return sb.toString()
    }

    String[] getCodes() {
        String[] codes = new String[1]
        codes[0] = msg.key
        return codes
    }

    /**
     * Return the array of arguments to be used to resolve this message.
     * @return an array of objects to be used as parameters to replace
     * placeholders within the message text
     * @see java.text.MessageFormat
     */
    Object[] getArguments() {
        Object[] av = new Object[args.size()]
        for(int i = 0; i<args.size(); i++) av[i] = args.get(i)
        return av
    }

    /**
     * Return the default message to be used to resolve this message.
     * @return the default message, or {@code null} if no default
     */
    String getDefaultMessage() {
        StringBuilder s = new StringBuilder()
        s.append(msg.name())
        if(args && args.size() > 0) {
            for(int i = 0; i<args.size(); i++) {
                s.append(i==0? ": {" : ", {")
                s.append(i)
                s.append("}")
            }
        }
        return s.toString()
    }

    protected void buildNestedString(StringBuilder sb, int depth) {
        sb.append(getSpringMessage())

        for (Object o : nested) {
            sb.append('\n')
            for (int i = 0; i < depth + 1; i++) {
                sb.append('\t')
            }

            if (o instanceof Message) {
                if(depth > 10) {
                    sb.append("(Depth exceeded)")
                }
                else {
                    ((Message) o).buildNestedString(sb, depth + 1)
                }
            } else if (o instanceof ServiceException) {
                sb.append(o.getClass().getSimpleName())
                sb.append(": ")
                ((ServiceException) o).msg.buildNestedString(sb, depth + 1)
            } else {
                for (int i = 0; i < depth + 1; i++) {
                    sb.append('\t')
                }
                sb.append(o)
            }
        }
    }

    private static String prefTitle(Name name) {
        return name.simpleName ?: name.fullName
    }

    private static String prefTitle(Reference reference) {
        return reference.abbrevTitle ?: reference.displayTitle ?: reference.title ?: reference.citation
    }

    private static String prefTitle(Instance instance) {
        String p = instance.page ? " p. ${instance.page}" : ''
        String pq = instance.pageQualifier ? " [${instance.pageQualifier}]" : ''
        return "${prefTitle(instance.name)} s. ${prefTitle(instance.reference)}${p}${pq}"
    }

    private static String prefTitle(Arrangement arrangement) {
        switch(arrangement.arrangementType) {
            case ArrangementType.U: return arrangement.title
            case ArrangementType.P: return arrangement.label
            default: "(${arrangement.arrangementType.name()}${arrangement.id})" // users should never see this
        }

        arrangement.label ?: arrangement.arrangementType.uriId
    }

    private static String prefTitle(Node node) {
        if(node.instance) {
            return "${prefTitle(node.instance)} (Name #${node.instance.name.id}) in ${prefTitle(node.root)} "
        }
        else
        if(node.name) {
            return "${prefTitle(node.name)} (Name #${node.name.id}) in ${prefTitle(node.root)} "
        }
        else {
            return node.typeUriIdPart ?: 'Node'
        }
    }

    private static String prefTitle(Message message) {
        message.toString()
    }

    private static String prefTitle(Object thing) {
        thing.toString()
    }

    String getSpringMessage() {
        // this does the job of deciding what our domain objects ought to look like when they appear in
        def args2 = args.collect { arg ->
            arg.hasProperty('id') ? prefTitle(arg) + " (${arg.id})" : prefTitle(arg)
        }

        return Holders.applicationContext.getMessage(
                msg.getKey(),
                args2.toArray(),
                msg.getKey() + args2,
                LocaleContextHolder.getLocale())
    }

    String getHumanReadableMessage() {
        // this does the job of deciding what our domain objects ought to look like when they appear in
        def args2 = args.collect { Object arg ->
            prefTitle(arg)
        }

        return Holders.applicationContext.getMessage(
                msg.getKey(),
                args2.toArray(),
                msg.getKey() + args2,
                LocaleContextHolder.getLocale())
    }
}