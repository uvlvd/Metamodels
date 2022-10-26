package org.xtext.lua.lua;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

public interface SuperChunk extends EObject {
  EList<Chunk> getChunks();
}
