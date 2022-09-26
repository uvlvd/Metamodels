/**
 * generated by Xtext 2.28.0
 */
package org.xtext.lua.scoping;

import org.eclipse.xtext.scoping.impl.SimpleLocalScopeProvider;

/**
 * This class contains custom scoping description. See
 * https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#scoping
 * on how and when to use it.
 */
@SuppressWarnings("all")
public class LuaScopeProviderOld extends SimpleLocalScopeProvider {
//  @Inject
//  private IQualifiedNameProvider qualifiedNameProvider;
//
//  private boolean isFunctionDeclaration(final EObject refble) {
//    if ((refble instanceof Referenceable)) {
//      return ((((Referenceable)refble).getLocalFunction() != null) || (((Referenceable)refble).getFunction() != null));
//    }
//    return false;
//  }
//
//  private void ingestParentArgs(final ArrayList<Referenceable> candidates, final EObject parent) {
//    if ((parent instanceof Referenceable)) {
//      ContainsBlock _localFunction = ((Referenceable)parent).getLocalFunction();
//      boolean _tripleNotEquals = (_localFunction != null);
//      if (_tripleNotEquals) {
//        final Function1<Referenceable, Boolean> _function = (Referenceable it) -> {
//          return Boolean.valueOf(candidates.add(it));
//        };
//        IterableExtensions.<Referenceable>forall(((Referenceable)parent).getLocalFunction().getArguments(), _function);
//        return;
//      }
//      ContainsBlock _function_1 = ((Referenceable)parent).getFunction();
//      boolean _tripleNotEquals_1 = (_function_1 != null);
//      if (_tripleNotEquals_1) {
//        final Function1<Referenceable, Boolean> _function_2 = (Referenceable it) -> {
//          return Boolean.valueOf(candidates.add(it));
//        };
//        IterableExtensions.<Referenceable>forall(((Referenceable)parent).getFunction().getArguments(), _function_2);
//        return;
//      }
//    }
//  }
//
//  private void ingestSibling(final ArrayList<Referenceable> candidates, final EObject sibling) {
//    if ((sibling instanceof Referenceable)) {
//      candidates.add(((Referenceable)sibling));
//      boolean _isFunctionDeclaration = this.isFunctionDeclaration(sibling);
//      boolean _not = (!_isFunctionDeclaration);
//      if (_not) {
//        final Function1<Referenceable, Boolean> _function = (Referenceable it) -> {
//          return Boolean.valueOf(candidates.add(it));
//        };
//        IterableExtensions.<Referenceable>forall(EcoreUtil2.<Referenceable>getAllContentsOfType(sibling, Referenceable.class), _function);
//      }
//      Expression _value = ((Referenceable)sibling).getValue();
//      boolean _tripleNotEquals = (_value != null);
//      if (_tripleNotEquals) {
//        Expression value = ((Referenceable)sibling).getValue();
//        if ((value instanceof Expression_VariableName)) {
//          String _name = ((Expression_VariableName)value).getRef().getName();
//          String _plus = (_name + " aliased to ");
//          String _name_1 = ((Referenceable)sibling).getName();
//          String _plus_1 = (_plus + _name_1);
//          InputOutput.<String>println(_plus_1);
//        }
//      }
//    }
//  }
//
//  private void ingestBlockExcluding(final ArrayList<Referenceable> candidates, final EObject excludedSibling) {
//    EObject previousSibling = EcoreUtil2.getPreviousSibling(excludedSibling);
//    while ((previousSibling != null)) {
//      {
//        this.ingestSibling(candidates, previousSibling);
//        previousSibling = EcoreUtil2.getPreviousSibling(previousSibling);
//      }
//    }
//  }
//
//  @Override
//  public IScope getScope(final EObject context, final EReference reference) {
//    SimpleScope _xblockexpression = null;
//    {
//      final ArrayList<Referenceable> candidates = new ArrayList<Referenceable>();
//      final ArrayList<IEObjectDescription> elements = CollectionLiterals.<IEObjectDescription>newArrayList();
//      EObject currentSibling = context;
//      boolean forceLevelAscend = false;
//      while ((currentSibling != null)) {
//        {
//          while (((currentSibling != null) && (forceLevelAscend || (!(currentSibling.eContainer() instanceof Block))))) {
//            {
//              currentSibling = currentSibling.eContainer();
//              forceLevelAscend = false;
//            }
//          }
//          if ((currentSibling != null)) {
//            this.ingestParentArgs(candidates, currentSibling);
//            this.ingestBlockExcluding(candidates, currentSibling);
//            forceLevelAscend = true;
//          }
//        }
//      }
//      final Consumer<Referenceable> _function = (Referenceable it) -> {
//        elements.add(
//          EObjectDescription.create(
//            this.qualifiedNameProvider.apply(it), it));
//      };
//      candidates.forEach(_function);
//      _xblockexpression = new SimpleScope(IScope.NULLSCOPE, elements);
//    }
//    return _xblockexpression;
//  }
}
